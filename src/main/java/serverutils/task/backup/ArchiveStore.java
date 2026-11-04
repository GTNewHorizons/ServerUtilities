package serverutils.task.backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;

import serverutils.ServerUtilities;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.compression.ICompress;

public final class ArchiveStore implements BackupStore<ArchiveEntry> {

    static final Pattern LEGACY_NAME = Pattern.compile("\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}(.*)");
    private static final DateTimeFormatter LEGACY_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss")
            .withResolverStyle(ResolverStyle.STRICT);

    private final File folder;

    public ArchiveStore(File folder) {
        this.folder = folder;
    }

    @Override
    public List<ArchiveEntry> list() throws IOException {
        File[] files = folder.listFiles();
        if (files == null) throw new IOException("Cannot list backup folder: " + folder);
        List<ArchiveEntry> archives = new ArrayList<>();
        for (File file : files) {
            BackupRetention.checkInterrupted();
            if (!file.getName().endsWith(".zip") || !Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS))
                continue;
            long size = Files.size(file.toPath());
            try {
                archives.add(read(file, size));
            } catch (IOException | RuntimeException e) {
                archives.add(
                        new ArchiveEntry(
                                file,
                                null,
                                file.lastModified(),
                                size,
                                false,
                                "Unrecognized/unreadable archive: " + e.getMessage()));
            }
        }
        BackupRetention.checkInterrupted();
        return archives;
    }

    @Override
    public void delete(Collection<ArchiveEntry> entries, BackupResult.Retention result) {
        for (ArchiveEntry entry : entries) {
            long size = entry.file.length();
            try {
                Files.delete(entry.file.toPath());
                ServerUtilities.LOGGER.info("Deleted old backup: {}", entry.file);
                result.filesRemoved++;
                result.bytesRemoved += size;
            } catch (IOException ex) {
                ServerUtilities.LOGGER.warn("Could not delete old backup {}", entry.file, ex);
            }
        }
    }

    static ArchiveEntry read(File file, long size) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            String worldPrefix = worldPrefix(zip);
            String worldName = worldPrefix.substring(0, worldPrefix.length() - 1);
            worldName = worldName.substring(worldName.lastIndexOf('/') + 1);
            ArchiveEntry metadata = readMetadata(file, zip, size);
            if (metadata != null) return metadata;
            String world = "name:" + worldName;
            // Existing SU archives already contain the persistent world UUID when universe.dat was saved.
            ZipEntry universe = zip.getEntry(worldPrefix + "serverutilities/universe.dat");
            if (universe != null) {
                try (DataInputStream input = new DataInputStream(new GZIPInputStream(zip.getInputStream(universe)))) {
                    String id = CompressedStreamTools.func_152456_a(input, new NBTSizeTracker(16 * 1024 * 1024L))
                            .getString("UUID");
                    if (!id.isEmpty()) {
                        // Universe saves UUIDs without hyphens.
                        UUID uuid = StringUtils.fromString(id);
                        if (uuid == null) throw new IOException("Invalid world UUID in universe.dat: " + id);
                        world = uuid.toString();
                    }
                }
            }
            long created = file.lastModified();
            boolean custom = !LEGACY_NAME.matcher(file.getName()).matches();
            if (!custom) {
                try {
                    created = LocalDateTime.parse(file.getName().substring(0, 19), LEGACY_DATE)
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                } catch (DateTimeParseException e) {
                    throw new IOException("Invalid timestamp in backup filename", e);
                }
            }
            return new ArchiveEntry(file, world, created, size, custom, null);
        }
    }

    private static ArchiveEntry readMetadata(File file, ZipFile zip, long size) throws IOException {
        ZipEntry metadata = zip.getEntry(ICompress.BACKUP_METADATA_ENTRY);
        if (metadata == null) return null;
        Properties properties = new Properties();
        try (InputStream input = zip.getInputStream(metadata)) {
            // Metadata is tiny; cap the read even when the ZIP's declared entry size is wrong.
            byte[] buffer = new byte[16_385];
            int length = 0;
            int read;
            while (length < buffer.length && (read = input.read(buffer, length, buffer.length - length)) != -1)
                length += read;
            if (length == buffer.length) throw new IOException("Backup metadata is too large");
            CRC32 checksum = new CRC32();
            checksum.update(buffer, 0, length);
            if (length != metadata.getSize() || checksum.getValue() != metadata.getCrc()) {
                throw new IOException("Backup metadata checksum or size mismatch");
            }
            properties.load(new ByteArrayInputStream(buffer, 0, length));
        }
        String custom = properties.getProperty("customName");
        if (!"1".equals(properties.getProperty("version")) || !("true".equals(custom) || "false".equals(custom))) {
            throw new IOException("Unsupported or invalid backup metadata");
        }
        String world = UUID.fromString(properties.getProperty("worldId", "")).toString();
        long created = Long.parseLong(properties.getProperty("createdAt", ""));
        if (created < 0) throw new IOException("Invalid backup timestamp");
        return new ArchiveEntry(file, world, created, size, Boolean.parseBoolean(custom), null);
    }

    static boolean isCustomWorldArchive(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            worldPrefix(zip);
            ArchiveEntry metadata = readMetadata(file, zip, file.length());
            return metadata == null ? !LEGACY_NAME.matcher(file.getName()).matches() : metadata.isCustom();
        }
    }

    private static String worldPrefix(ZipFile zip) throws IOException {
        String worldName = zip.getComment();
        boolean hasComment = worldName != null && !worldName.isEmpty();
        if (hasComment && (worldName.equals(".") || worldName.equals("..")
                || worldName.contains("/")
                || worldName.contains("\\")
                || worldName.contains(":"))) {
            throw new IOException("Invalid world identifier");
        }
        Set<String> prefixes = new HashSet<>();
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            BackupRetention.checkInterrupted();
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || entry.getSize() <= 0) continue;
            String name = entry.getName();
            int separator = name.lastIndexOf('/');
            if (separator < 0) continue;
            String filename = name.substring(separator + 1);
            if (!filename.equals("level.dat") && !filename.equals("level.dat_old")) continue;
            String prefix = name.substring(0, separator + 1);
            if (hasComment && !prefix.equals(worldName + "/") && !prefix.endsWith("/" + worldName + "/")) continue;
            if (prefix.startsWith("/") || prefix.contains("\\")
                    || prefix.contains(":")
                    || prefix.contains("//")
                    || prefix.matches("(^|.*/)\\.{1,2}(/.*|$)")) {
                throw new IOException("Unsafe world folder: " + prefix);
            }
            prefixes.add(prefix);
        }
        if (prefixes.size() != 1) {
            throw new IOException(
                    !prefixes.isEmpty() ? "Backup contains multiple world folders for world " + worldName
                            : "Backup contains no level.dat or level.dat_old for world " + worldName);
        }
        return prefixes.iterator().next();
    }

    static void writeMetadata(ICompress compressor, String worldId, long created, boolean custom) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("version", "1");
        properties.setProperty("worldId", worldId);
        properties.setProperty("createdAt", Long.toString(created));
        properties.setProperty("customName", Boolean.toString(custom));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        properties.store(output, "ServerUtilities backup metadata; not restored");
        byte[] bytes = output.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(bytes);
        ZipEntry entry = new ZipEntry(ICompress.BACKUP_METADATA_ENTRY);
        entry.setSize(bytes.length);
        entry.setCrc(crc.getValue());
        entry.setTime(created);
        entry.setMethod(ZipEntry.STORED);
        compressor.addStreamToArchive(new ByteArrayInputStream(bytes), entry);
    }
}
