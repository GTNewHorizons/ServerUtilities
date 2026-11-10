package serverutils.task.backup.repository;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Command line access to an incremental backup repository, for restoring without starting Minecraft. It is the jar's
 * main class: {@code java -jar ServerUtilities.jar <command>}. Only JDK classes may be used from here.
 */
public final class RepositoryTool {

    /** Same entry name as ICompress.BACKUP_METADATA_ENTRY, which cannot be loaded outside Minecraft. */
    static final String ZIP_METADATA_ENTRY = ".serverutilities-backup.properties";

    private static final String USAGE = """
            ServerUtilities incremental backup tool

            Usage: java -jar ServerUtilities.jar [--repo <folder>] [--wait <seconds>] <command> [options]

            The repository defaults to backups/incremental in the current folder, or the current folder itself if it
            is a repository. Run from the server folder, or pass --repo. <archive> is an archive name as shown by
            'list', or 'latest'.

            Commands:
              list                         List archives, oldest first
              info <archive>               Show details of an archive
              files <archive> [glob]       List the files in an archive, optionally only paths matching the glob
              restore <archive> <target>   Restore an archive's files into the target folder, usually the server folder
                  --world-only             Only restore the world folder, not global files such as ranks or NEI data
                  --include <glob>         Only restore paths matching the glob, e.g. 'World/region/*' (repeatable)
                  --replace-world          Move an existing world folder to <world>_old first, like the
                                           singleplayer Backups screen, so no old files are left in the world
                  --overwrite              Replace other existing files; the originals are moved to
                                           <target>/backups_before_restore/
                  --dry-run                Show what would be restored without writing anything
              export <archive> <file.zip>  Write the archive as a regular ServerUtilities ZIP backup, which can also be
                                           restored from the singleplayer Backups screen
              check [--verify-data]        Check that every archive is readable and complete; --verify-data also reads
                                           and verifies every stored chunk
              delete <archive>...          Delete archives and free the space no other archive uses

            Stop the server before restoring into its folder. Reading commands can run while the server is up; they
            wait up to --wait seconds (default 60) while a backup is being written.
            """;

    private final PrintStream out;
    private final PrintStream err;
    private File repositoryFolder;
    private long waitMillis = 60_000;

    public RepositoryTool(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public static void main(String[] args) {
        System.exit(new RepositoryTool(System.out, System.err).run(args));
    }

    private static final class UsageException extends Exception {

        UsageException(String message) {
            super(message);
        }
    }

    /** Runs one command and returns the process exit code. */
    public int run(String[] arguments) {
        List<String> args = new ArrayList<>();
        try {
            for (int i = 0; i < arguments.length; i++) {
                String arg = arguments[i];
                if (arg.equals("--repo")) repositoryFolder = new File(value(arguments, ++i, arg));
                else if (arg.equals("--wait")) {
                    try {
                        waitMillis = Math.multiplyExact(Long.parseLong(value(arguments, ++i, arg)), 1000L);
                    } catch (NumberFormatException | ArithmeticException e) {
                        throw new UsageException("Invalid --wait value");
                    }
                } else args.add(arg);
            }
            if (args.isEmpty() || args.get(0).equals("help") || args.contains("--help") || args.contains("-h")) {
                out.print(USAGE);
                return args.isEmpty() ? 2 : 0;
            }
            if (repositoryFolder == null) repositoryFolder = defaultRepository();
            String command = args.remove(0);
            return switch (command) {
                case "list" -> list(args);
                case "info" -> info(args);
                case "files" -> files(args);
                case "restore" -> restore(args);
                case "export" -> export(args);
                case "check" -> check(args);
                case "delete" -> delete(args);
                default -> throw new UsageException("Unknown command: " + command);
            };
        } catch (UsageException e) {
            err.println(e.getMessage());
            err.println("Run without arguments for usage.");
            return 2;
        } catch (IOException | RuntimeException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private static String value(String[] args, int index, String option) throws UsageException {
        if (index >= args.length) throw new UsageException("Missing value for " + option);
        return args[index];
    }

    private static File defaultRepository() {
        File current = new File(".");
        return Repository.exists(current) ? current : new File("backups/incremental");
    }

    private Repository open(boolean writable) throws IOException {
        Repository repository = Repository.open(repositoryFolder, writable, waitMillis);
        for (String warning : repository.warnings()) err.println("Warning: " + warning);
        return repository;
    }

    private static void expect(List<String> args, int min, int max) throws UsageException {
        if (args.size() < min) throw new UsageException("Missing arguments");
        if (args.size() > max) throw new UsageException("Unexpected argument: " + args.get(max));
        for (String arg : args) {
            if (arg.startsWith("--")) throw new UsageException("Unknown option: " + arg);
        }
    }

    private static ArchiveInfo archive(Repository repository, String name) throws IOException {
        if (name.endsWith(Repository.ARCHIVE_EXTENSION)) {
            name = name.substring(0, name.length() - Repository.ARCHIVE_EXTENSION.length());
        }
        if (name.equals("latest")) {
            ArchiveInfo latest = null;
            for (String candidate : repository.archiveNames()) {
                ArchiveInfo info = repository.readInfo(candidate);
                if (latest == null || info.created() >= latest.created()) latest = info;
            }
            if (latest == null) throw new IOException("The repository has no archives");
            return latest;
        }
        if (name.isEmpty() || name.contains("/") || name.contains("\\")) {
            throw new IOException("Invalid archive name: " + name);
        }
        if (!repository.archiveFile(name).isFile()) throw new IOException("No archive named " + name);
        return repository.readInfo(name);
    }

    private int list(List<String> args) throws IOException, UsageException {
        expect(args, 0, 0);
        try (Repository repository = open(false)) {
            List<ArchiveInfo> archives = new ArrayList<>();
            for (String name : repository.archiveNames()) {
                try {
                    archives.add(repository.readInfo(name));
                } catch (IOException e) {
                    err.println("Unreadable archive " + name + ": " + e.getMessage());
                }
            }
            archives.sort(Comparator.comparingLong(ArchiveInfo::created).thenComparing(info -> info.name));
            for (ArchiveInfo info : archives) {
                out.printf(
                        "%-32s %s  %8s  %7d files%s%n",
                        info.name,
                        date(info.created()),
                        size(info.originalSize()),
                        info.fileCount(),
                        info.worldName() == null ? "" : "  " + info.worldName());
            }
            out.printf(
                    "%d archives, %s on disk in %d packs, %d unique chunks%n",
                    archives.size(),
                    size(repository.diskSize()),
                    repository.packCount(),
                    repository.chunkCount());
        }
        return 0;
    }

    private int info(List<String> args) throws IOException, UsageException {
        expect(args, 1, 1);
        try (Repository repository = open(false)) {
            ArchiveInfo info = archive(repository, args.get(0));
            Repository.Usage own = repository.usage();
            own.add(info);
            Repository.Usage others = repository.usage();
            for (String name : repository.archiveNames()) {
                if (name.equals(info.name)) continue;
                try {
                    others.add(repository.readInfo(name));
                } catch (IOException e) {
                    err.println("Unreadable archive " + name + ": " + e.getMessage());
                }
            }
            long shared = others.liveBytes();
            others.add(info);
            out.println("Archive:       " + info.name);
            out.println("Created:       " + date(info.created()));
            out.println(
                    "World:         " + (info.worldName() == null ? "?" : info.worldName())
                            + (info.worldPath() == null ? "" : " (" + info.worldPath() + ")"));
            if (info.worldId() != null) out.println("World id:      " + info.worldId());
            out.println("Custom name:   " + info.isCustomName());
            out.println("Files:         " + info.fileCount());
            out.println("Original size: " + size(info.originalSize()));
            out.println("Stored size:   " + size(own.liveBytes()) + " (deduplicated and compressed)");
            out.println("Unique size:   " + size(others.liveBytes() - shared) + " (freed by deleting this archive)");
        }
        return 0;
    }

    private int files(List<String> args) throws IOException, UsageException {
        expect(args, 1, 2);
        PathMatcher matcher = args.size() > 1 ? glob(args.get(1)) : null;
        try (Repository repository = open(false)) {
            ArchiveInfo info = archive(repository, args.get(0));
            repository.readItems(info, item -> {
                if (matcher == null || matcher.matches(Paths.get(item.path))) {
                    out.printf("%10s  %s  %s%n", size(item.size), date(item.modified), item.path);
                }
            });
        }
        return 0;
    }

    private static PathMatcher glob(String pattern) {
        return FileSystems.getDefault().getPathMatcher("glob:" + pattern.replace('\\', '/'));
    }

    private int restore(List<String> args) throws IOException, UsageException {
        RepositoryRestore restore = new RepositoryRestore();
        restore.progress = out::println;
        boolean dryRun = false;
        List<String> positional = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            switch (arg) {
                case "--world-only" -> restore.worldOnly = true;
                case "--overwrite" -> restore.overwrite = true;
                case "--replace-world" -> restore.replaceWorld = true;
                case "--dry-run" -> dryRun = true;
                case "--include" -> {
                    if (++i >= args.size()) throw new UsageException("Missing value for --include");
                    restore.includes.add(glob(args.get(i)));
                }
                default -> positional.add(arg);
            }
        }
        expect(positional, 2, 2);
        Path root = Paths.get(positional.get(1)).toAbsolutePath().normalize();
        try (Repository repository = open(false)) {
            ArchiveInfo info = archive(repository, positional.get(0));
            RepositoryRestore.Plan plan = restore.plan(repository, info, root);
            out.printf(
                    "Restoring %d files (%s) from %s into %s%n",
                    plan.items.size(),
                    size(plan.totalSize),
                    info.name,
                    root);
            if (!plan.conflicts.isEmpty() && !restore.overwrite) {
                err.println(plan.conflicts.size() + " files already exist in the target, for example:");
                for (String conflict : plan.conflicts.subList(0, Math.min(10, plan.conflicts.size())))
                    err.println("  " + conflict);
                err.println(
                        "Restore into an empty folder, pass --replace-world to move the existing world aside,"
                                + " or pass --overwrite.");
                return 1;
            }
            if (plan.existingWorld != null)
                out.println("The existing world will be moved to " + plan.existingWorld + "_old");
            else if (restore.overwrite && info.worldPath() != null
                    && Files.isDirectory(root.resolve(info.worldPath()))) {
                        err.println(
                                "Warning: files in the existing world folder that are not in the backup are left in place."
                                        + " Pass --replace-world for an exact restore.");
                    }
            if (dryRun) {
                for (Item item : plan.items) out.println("  " + item.path);
                out.println("Dry run: nothing was written.");
                return 0;
            }
            RepositoryRestore.Result result = restore.restore(repository, info, root);
            out.println("Restored " + result.files + " files.");
            if (result.previousWorld != null) out.println("The previous world was moved to " + result.previousWorld);
            if (result.replacedFiles != null) out.println("Replaced files were moved to " + result.replacedFiles);
        }
        return 0;
    }

    private int export(List<String> args) throws IOException, UsageException {
        boolean overwrite = args.remove("--overwrite");
        expect(args, 2, 2);
        File destination = new File(args.get(1)).getAbsoluteFile();
        if (destination.exists() && !overwrite) {
            throw new IOException(destination + " already exists; pass --overwrite to replace it");
        }
        try (Repository repository = open(false)) {
            ArchiveInfo info = archive(repository, args.get(0));
            File parent = destination.getParentFile();
            Files.createDirectories(parent.toPath());
            Path temporary = Files.createTempFile(parent.toPath(), ".su-export-", ".tmp");
            try {
                int[] count = { 0 };
                try (FileOutputStream file = new FileOutputStream(temporary.toFile());
                        ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(file, 1024 * 1024))) {
                    zip.setLevel(1);
                    if (info.worldName() != null) zip.setComment(info.worldName());
                    if (info.worldId() != null) writeZipMetadata(zip, info);
                    byte[] buffer = new byte[64 * 1024];
                    repository.readItems(info, item -> {
                        ZipEntry entry = new ZipEntry(item.path);
                        entry.setTime(item.modified);
                        zip.putNextEntry(entry);
                        CRC32 crc = new CRC32();
                        long size = 0;
                        try (InputStream input = repository.openItem(item)) {
                            int read;
                            while ((read = input.read(buffer)) != -1) {
                                crc.update(buffer, 0, read);
                                zip.write(buffer, 0, read);
                                size += read;
                            }
                        }
                        if (size != item.size || (int) crc.getValue() != item.crc) {
                            throw new IOException("Size or checksum mismatch while exporting " + item.path);
                        }
                        zip.closeEntry();
                        count[0]++;
                    });
                    zip.finish();
                    zip.flush();
                    file.getFD().sync();
                }
                Files.move(temporary, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                out.println(
                        "Exported " + count[0] + " files to " + destination + " (" + size(destination.length()) + ")");
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        return 0;
    }

    /** The same metadata a ZIP backup carries, so retention and restore treat the export like the original. */
    private static void writeZipMetadata(ZipOutputStream zip, ArchiveInfo info) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("version", "1");
        properties.setProperty("worldId", info.worldId());
        properties.setProperty("createdAt", Long.toString(info.created()));
        properties.setProperty("customName", Boolean.toString(info.isCustomName()));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        properties.store(buffer, "ServerUtilities backup metadata; not restored");
        byte[] bytes = buffer.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(bytes);
        ZipEntry entry = new ZipEntry(ZIP_METADATA_ENTRY);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(bytes.length);
        entry.setCrc(crc.getValue());
        entry.setTime(info.created());
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private int check(List<String> args) throws IOException, UsageException {
        boolean verifyData = args.remove("--verify-data");
        expect(args, 0, 0);
        try (Repository repository = open(false)) {
            Repository.CheckResult result = repository.check(verifyData, out::println);
            out.printf(
                    "%d archives checked, %d damaged, %d missing chunk references%s%n",
                    result.archives,
                    result.brokenArchives,
                    result.missingChunks,
                    verifyData ? ", " + result.checkedChunks + " chunks verified, " + result.corruptChunks + " corrupt"
                            : "");
            out.println(result.ok() ? "Repository OK" : "Repository has errors");
            return result.ok() ? 0 : 1;
        }
    }

    private int delete(List<String> args) throws IOException, UsageException {
        expect(args, 1, Integer.MAX_VALUE);
        try (Repository repository = open(true)) {
            List<ArchiveInfo> archives = new ArrayList<>();
            for (String name : args) {
                if (name.equals("latest")) throw new UsageException("Name the archive to delete explicitly");
                archives.add(archive(repository, name));
            }
            for (ArchiveInfo info : archives) {
                repository.deleteArchive(info.name);
                out.println("Deleted " + info.name);
            }
            Repository.CompactResult result = repository.compact(0.1);
            for (String warning : repository.warnings()) err.println("Warning: " + warning);
            out.println("Freed " + size(result.freedBytes));
        }
        return 0;
    }

    private static String date(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(millis));
    }

    static String size(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] units = { "KB", "MB", "GB", "TB" };
        double value = bytes;
        int unit = -1;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }
}
