package serverutils.task.backup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

import serverutils.lib.util.compression.ICompress;

/** Selects complete, independent ZIP backups. Chained archives would also need dependency preservation. */
public final class BackupRetention {

    static final Pattern LEGACY_NAME = Pattern.compile("\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}(.*)");
    private static final DateTimeFormatter LEGACY_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss")
            .withResolverStyle(ResolverStyle.STRICT);
    // 1970-01-05 was a Monday; day-sized buckets share its UTC midnight alignment.
    private static final long BUCKET_ORIGIN = 4 * 86_400_000L;

    private BackupRetention() {}

    static final class Rule {

        final String text;
        final long age;
        final long interval;

        Rule(String text, long age, long interval) {
            this.text = text;
            this.age = age;
            this.interval = interval;
        }
    }

    static List<Rule> parse(String[] policy) {
        List<Rule> rules = new ArrayList<>();
        for (String value : policy) {
            String[] parts = value == null ? new String[0] : value.trim().split(":", -1);
            if (parts.length != 2) throw new IllegalArgumentException("Invalid retention rule: " + value);
            String age = parts[0].trim();
            String interval = parts[1].trim();
            rules.add(
                    new Rule(
                            age + ":" + interval,
                            age.equals("forever") ? Long.MAX_VALUE : BackupDuration.parse(age),
                            interval.equals("all") ? 0 : BackupDuration.parse(interval)));
        }
        return rules;
    }

    public static final class Archive {

        public final File file;
        final String world;
        public final long created;
        public final long size;
        final boolean custom;
        final String problem;

        Archive(File file, String world, long created, long size, boolean custom, String problem) {
            this.file = file;
            this.world = world;
            this.created = created;
            this.size = size;
            this.custom = custom;
            this.problem = problem;
        }
    }

    public static final class Plan {

        public final Map<File, String> keep = new LinkedHashMap<>();
        public final Map<File, String> delete = new LinkedHashMap<>();
        final Set<File> sizeCandidates = new LinkedHashSet<>();
        final Set<File> sizeExempt = new HashSet<>();
        public long remainingSize;
        public long remainingRotationSize;

        private void keep(File file, String reason) {
            keep.merge(file, reason, (previous, added) -> previous + "; " + added);
        }
    }

    public static Plan plan(File folder, String[] policy, long now, boolean deleteCustom, long maxSize)
            throws IOException {
        List<Rule> rules = parse(policy);
        Plan plan = select(readArchives(folder), rules, now, deleteCustom, maxSize);
        checkInterrupted();
        return plan;
    }

    static List<Archive> readArchives(File folder) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) throw new IOException("Cannot list backup folder: " + folder);
        List<Archive> archives = new ArrayList<>();
        for (File file : files) {
            checkInterrupted();
            if (!file.getName().endsWith(".zip") || !Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS))
                continue;
            long size = Files.size(file.toPath());
            try {
                archives.add(read(file, size));
            } catch (IOException | RuntimeException e) {
                archives.add(
                        new Archive(
                                file,
                                null,
                                file.lastModified(),
                                size,
                                false,
                                "Unrecognized/unreadable archive: " + e.getMessage()));
            }
        }
        checkInterrupted();
        return archives;
    }

    static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Backup retention cancelled");
    }

    static Plan select(List<Archive> input, List<Rule> rules, long now, boolean deleteCustom, long maxSize) {
        List<Archive> archives = new ArrayList<>(input);
        archives.sort(
                Comparator.comparingLong((Archive archive) -> archive.created).reversed()
                        .thenComparing(archive -> archive.file.getName()));
        Plan plan = new Plan();
        Set<File> protectedFiles = new HashSet<>();
        Set<String> worlds = new HashSet<>();
        for (Archive archive : archives) {
            plan.remainingSize = Math.addExact(plan.remainingSize, archive.size);
            if (archive.problem != null || archive.created > now || (!deleteCustom && archive.custom)) {
                plan.sizeExempt.add(archive.file);
                plan.keep(
                        archive.file,
                        archive.problem != null ? archive.problem
                                : archive.created > now ? "Timestamp is in the future" : "Protected custom backup");
                protectedFiles.add(archive.file);
                continue;
            }
            if (worlds.add(archive.world)) {
                plan.keep(archive.file, "Latest backup for world");
                protectedFiles.add(archive.file);
            }
        }
        for (Rule rule : rules) {
            Map<String, Set<Long>> buckets = new HashMap<>();
            for (Archive archive : archives) {
                if (archive.problem != null || archive.created > now
                        || (!deleteCustom && archive.custom)
                        || now - archive.created > rule.age)
                    continue;
                if (rule.interval == 0 || buckets.computeIfAbsent(archive.world, key -> new HashSet<>())
                        .add(Math.floorDiv(archive.created - BUCKET_ORIGIN, rule.interval))) {
                    plan.keep(archive.file, rule.text);
                    if (rule.age == Long.MAX_VALUE) {
                        protectedFiles.add(archive.file);
                        plan.sizeExempt.add(archive.file);
                    }
                }
            }
        }
        for (Archive archive : archives) {
            if (!plan.keep.containsKey(archive.file)) {
                plan.delete.put(archive.file, "Outside retention policy or superseded in time bucket");
                plan.remainingSize -= archive.size;
            } else if (!plan.sizeExempt.contains(archive.file)) {
                plan.remainingRotationSize = Math.addExact(plan.remainingRotationSize, archive.size);
            }
        }
        // Only finite-retention archives may be sacrificed to the size limit, oldest first.
        for (int i = archives.size() - 1; i >= 0; i--) {
            Archive archive = archives.get(i);
            if (protectedFiles.contains(archive.file) || !plan.keep.containsKey(archive.file)) continue;
            plan.sizeCandidates.add(archive.file);
            if (maxSize <= 0 || plan.remainingRotationSize <= maxSize) continue;
            plan.keep.remove(archive.file);
            plan.delete.put(archive.file, "Folder size limit");
            plan.remainingSize -= archive.size;
            plan.remainingRotationSize -= archive.size;
        }
        return plan;
    }

    static Archive read(File file, long size) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            String worldPrefix = worldPrefix(zip);
            String worldName = worldPrefix.substring(0, worldPrefix.length() - 1);
            worldName = worldName.substring(worldName.lastIndexOf('/') + 1);
            Archive metadata = readMetadata(file, zip, size);
            if (metadata != null) return metadata;
            String world = "name:" + worldName;
            // Existing SU archives already contain the persistent world UUID when universe.dat was saved.
            ZipEntry universe = zip.getEntry(worldPrefix + "serverutilities/universe.dat");
            if (universe != null) {
                try (DataInputStream input = new DataInputStream(new GZIPInputStream(zip.getInputStream(universe)))) {
                    String id = CompressedStreamTools.func_152456_a(input, new NBTSizeTracker(16 * 1024 * 1024L))
                            .getString("UUID");
                    if (!id.isEmpty()) world = UUID.fromString(id).toString();
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
            return new Archive(file, world, created, size, custom, null);
        }
    }

    private static Archive readMetadata(File file, ZipFile zip, long size) throws IOException {
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
        return new Archive(file, world, created, size, Boolean.parseBoolean(custom), null);
    }

    static boolean isCustomWorldArchive(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file)) {
            worldPrefix(zip);
            Archive metadata = readMetadata(file, zip, file.length());
            return metadata == null ? !LEGACY_NAME.matcher(file.getName()).matches() : metadata.custom;
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
            checkInterrupted();
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
