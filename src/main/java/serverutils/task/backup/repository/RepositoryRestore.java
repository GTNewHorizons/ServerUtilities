package serverutils.task.backup.repository;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;

/**
 * Restores an archive's files into a folder. The repository tool and the singleplayer Backups screen both restore
 * through this class, so they behave the same. If anything fails, every file written so far is removed and every file
 * moved out of the way is put back.
 */
public final class RepositoryRestore {

    /** Only restore the world folder, not global files such as ranks or NEI data. */
    public boolean worldOnly;
    /** Replace existing files, moving the originals to {@code backups_before_restore/restore-<time>/}. */
    public boolean overwrite;
    /** Move an existing world folder to {@code <world>_old} first, so the restored world has no leftover files. */
    public boolean replaceWorld;
    /** If not empty, only paths matching one of these are restored. */
    public final List<PathMatcher> includes = new ArrayList<>();
    public Repository.Progress progress = message -> {};

    /** What a restore would write, and which existing files are in the way. */
    public static final class Plan {

        public final List<Item> items = new ArrayList<>();
        final List<Path> targets = new ArrayList<>();
        /** Archive paths of files that already exist, not counting a world folder that will be moved aside. */
        public final List<String> conflicts = new ArrayList<>();
        public long totalSize;
        /** The existing world folder that {@link #replaceWorld} moves aside, or null. */
        public Path existingWorld;
    }

    public static final class Result {

        public int files;
        public long size;
        /** Where replaced files were moved, or null if none were. */
        public Path replacedFiles;
        /** Where the previous world folder was moved, or null. */
        public Path previousWorld;
    }

    /** Thrown when files are in the way and {@link #overwrite} is off. Nothing has been written. */
    public static final class ConflictException extends IOException {

        public final List<String> conflicts;

        ConflictException(List<String> conflicts) {
            super(conflicts.size() + " files already exist in the restore target, for example " + conflicts.get(0));
            this.conflicts = conflicts;
        }
    }

    public Plan plan(Repository repository, ArchiveInfo archive, Path root) throws IOException {
        root = root.toAbsolutePath().normalize();
        String worldPath = archive.worldPath();
        if ((worldOnly || replaceWorld) && worldPath == null) {
            throw new IOException("The world folder of " + archive.name + " is unknown");
        }
        Plan plan = new Plan();
        repository.readItems(archive, item -> {
            if (worldOnly && !item.path.startsWith(worldPath)) return;
            if (!includes.isEmpty() && includes.stream().noneMatch(m -> m.matches(Paths.get(item.path)))) return;
            plan.items.add(item);
        });
        if (plan.items.isEmpty()) throw new IOException("No files in " + archive.name + " match the selection");
        if (replaceWorld) {
            Path world = restoreTarget(root, worldPath.substring(0, worldPath.length() - 1));
            if (Files.exists(world, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isDirectory(world, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("World folder is not a directory: " + world);
                }
                plan.existingWorld = world;
            }
        }
        for (Item item : plan.items) {
            Path target = restoreTarget(root, item.path);
            plan.targets.add(target);
            plan.totalSize += item.size;
            if (plan.existingWorld != null && item.path.startsWith(worldPath)) continue;
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Restore target exists and is not a regular file: " + target);
                }
                plan.conflicts.add(item.path);
            }
        }
        return plan;
    }

    public Result restore(Repository repository, ArchiveInfo archive, Path root) throws IOException {
        Plan plan = plan(repository, archive, root);
        if (!plan.conflicts.isEmpty() && !overwrite) throw new ConflictException(plan.conflicts);
        Files.createDirectories(root);
        Path realRoot = root.toRealPath();
        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ROOT).format(new Date());
        Result result = new Result();
        Path displacedRoot = plan.conflicts.isEmpty() ? null
                : realRoot.resolve("backups_before_restore").resolve("restore-" + time);
        List<Path> installed = new ArrayList<>();
        List<Path[]> displaced = new ArrayList<>();
        try {
            if (plan.existingWorld != null) {
                Path previous = plan.existingWorld.resolveSibling(plan.existingWorld.getFileName() + "_old");
                while (Files.exists(previous, LinkOption.NOFOLLOW_LINKS)) {
                    previous = previous.resolveSibling(previous.getFileName() + "_old");
                }
                Files.move(plan.existingWorld, previous);
                result.previousWorld = previous;
                progress.report("Moved the previous world to " + previous);
            }
            long lastReport = System.currentTimeMillis();
            for (int i = 0; i < plan.items.size(); i++) {
                Item item = plan.items.get(i);
                Path target = plan.targets.get(i);
                Path original = restoreItem(repository, item, realRoot, target, displacedRoot);
                if (original != null) displaced.add(new Path[] { target, original });
                installed.add(target);
                result.files++;
                result.size += item.size;
                if (System.currentTimeMillis() - lastReport > 5000) {
                    lastReport = System.currentTimeMillis();
                    progress.report(
                            String.format(
                                    "  %d/%d files, %s of %s",
                                    i + 1,
                                    plan.items.size(),
                                    RepositoryTool.size(result.size),
                                    RepositoryTool.size(plan.totalSize)));
                }
            }
        } catch (IOException | RuntimeException failure) {
            rollBack(failure, installed, displaced, plan.existingWorld, result.previousWorld);
            throw failure;
        }
        if (!displaced.isEmpty()) result.replacedFiles = displacedRoot;
        return result;
    }

    private static void rollBack(Exception failure, List<Path> installed, List<Path[]> displaced, Path world,
            Path previousWorld) {
        Collections.reverse(installed);
        for (Path file : installed) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                failure.addSuppressed(e);
            }
        }
        Collections.reverse(displaced);
        for (Path[] move : displaced) {
            try {
                Files.move(move[1], move[0]);
            } catch (IOException e) {
                failure.addSuppressed(e);
            }
        }
        if (previousWorld != null) {
            try {
                if (Files.exists(world, LinkOption.NOFOLLOW_LINKS)) deleteTree(world);
                Files.move(previousWorld, world);
            } catch (IOException e) {
                failure.addSuppressed(e);
                failure.addSuppressed(new IOException("The previous world is still in " + previousWorld));
            }
        }
    }

    private static void deleteTree(Path folder) throws IOException {
        Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException failure) throws IOException {
                if (failure != null) throw failure;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Rejects paths that would leave the target folder. */
    static Path restoreTarget(Path root, String path) throws IOException {
        if (path.isEmpty() || path.startsWith("/")
                || path.contains("\\")
                || path.contains(":")
                || path.matches("(^|.*/)\\.{1,2}(/.*|$)")
                || path.contains("//")
                || path.endsWith("/")) {
            throw new IOException("Unsafe path in archive: " + path);
        }
        Path target = root.resolve(path).normalize();
        if (!target.startsWith(root) || target.equals(root)) throw new IOException("Unsafe path in archive: " + path);
        return target;
    }

    /** Writes one file through a verified temporary, and returns where a replaced original was moved, or null. */
    private static Path restoreItem(Repository repository, Item item, Path realRoot, Path target, Path displacedRoot)
            throws IOException {
        Path parent = target.getParent();
        Files.createDirectories(parent);
        // A link inside the target folder must not redirect files outside it.
        if (!parent.toRealPath().startsWith(realRoot)) {
            throw new IOException("Restore path leaves the target: " + target);
        }
        Path temporary = Files.createTempFile(parent, ".su-restore-", ".tmp");
        try {
            CRC32 crc = new CRC32();
            long size = 0;
            byte[] buffer = new byte[64 * 1024];
            try (InputStream input = repository.openItem(item);
                    FileOutputStream file = new FileOutputStream(temporary.toFile());
                    OutputStream output = new BufferedOutputStream(file, 1024 * 1024)) {
                int read;
                while ((read = input.read(buffer)) != -1) {
                    crc.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                    size += read;
                }
                output.flush();
                file.getFD().sync();
            }
            if (size != item.size || (int) crc.getValue() != item.crc) {
                throw new IOException("Size or checksum mismatch while restoring " + item.path);
            }
            Files.setLastModifiedTime(temporary, FileTime.fromMillis(item.modified));
            Path original = null;
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (displacedRoot == null) throw new IOException("Restore target appeared during restore: " + target);
                original = displacedRoot.resolve(realRoot.relativize(target.toRealPath()));
                Files.createDirectories(original.getParent());
                Files.move(target, original);
            }
            try {
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, target);
                }
            } catch (IOException e) {
                if (original != null) Files.move(original, target);
                throw e;
            }
            return original;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
