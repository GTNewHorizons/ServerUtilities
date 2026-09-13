package serverutils.lib.util.compression;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import serverutils.lib.util.FileUtils;

final class ArchiveExtraction {

    static boolean isOldBackup(File archive) throws IOException {
        try (ZipFile zip = new ZipFile(archive)) {
            String worldName = zip.getComment();
            boolean dedicated = false;
            boolean singlePlayer = false;
            boolean anySaves = false;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                Path path = Paths.get(entries.nextElement().getName().replace('\\', '/')).normalize();
                anySaves |= path.startsWith("saves");
                if (worldName != null && !worldName.isEmpty()) {
                    dedicated |= path.equals(Paths.get(worldName, "level.dat"))
                            || path.equals(Paths.get(worldName, "level.dat_old"));
                    singlePlayer |= path.equals(Paths.get("saves", worldName, "level.dat"))
                            || path.equals(Paths.get("saves", worldName, "level.dat_old"));
                }
            }
            if (dedicated && singlePlayer) throw new IOException("Backup contains two layouts for world " + worldName);
            if (dedicated || singlePlayer) return dedicated;
            // Archives without identifiable world metadata retain the original layout detection.
            return !anySaves;
        }
    }

    static void validateRestoreTargets(File archive, String worldName, boolean legacy) throws IOException {
        validateRestoreTargets(archive, worldName, legacy, true);
    }

    static void validateRestoreTargets(File archive, String worldName, boolean legacy, boolean includeGlobal)
            throws IOException {
        try (ZipFile zip = new ZipFile(archive)) {
            boolean hasWorldMetadata = false;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                Path path = restorePath(entry.getName(), legacy, worldName);
                hasWorldMetadata |= path.equals(Paths.get("saves", worldName, "level.dat"))
                        || path.equals(Paths.get("saves", worldName, "level.dat_old"));
                if (includeGlobal && !isAllowedTarget(path, worldName)) {
                    throw new IOException(
                            "Backup contains an unconfigured restore target: " + path
                                    + "; restore the world only or enable the original additional_backup_files pattern");
                }
            }
            if (!hasWorldMetadata) {
                throw new IOException("Backup contains no level.dat or level.dat_old for world " + worldName);
            }
        }
    }

    private static boolean isAllowedTarget(Path path, String worldName) {
        if (path.startsWith(Paths.get("saves", worldName).normalize()) || isRankFile(path)) return true;
        for (String pattern : backups.additional_backup_files) {
            if (FileUtils.matchesBackupPath(path, pattern.replace("$WORLDNAME", worldName))) return true;
        }
        return false;
    }

    private static boolean isRankFile(Path relative) {
        return relative.equals(Paths.get(serverutils.ServerUtilities.SERVER_FOLDER, "ranks.txt"))
                || relative.equals(Paths.get(serverutils.ServerUtilities.SERVER_FOLDER, "players.txt"));
    }

    private static Path restorePath(String name, boolean legacy, String worldName) {
        Path path = Paths.get(name.replace('\\', '/')).normalize();
        // Dedicated backups use a world-relative prefix alongside instance-relative global files.
        if (legacy && !isRankFile(path) && (worldName == null || path.startsWith(Paths.get(worldName)))) {
            return Paths.get("saves").resolve(path);
        }
        return path;
    }

    static void extract(File archive, boolean includeGlobal, boolean legacy, File preserved) throws IOException {
        extract(archive, includeGlobal, legacy, Paths.get("").toAbsolutePath(), preserved);
    }

    static void extract(File archive, boolean includeGlobal, boolean legacy, Path root) throws IOException {
        extract(archive, includeGlobal, legacy, root, null);
    }

    static void extract(File archive, boolean includeGlobal, boolean legacy, Path root, File preserved)
            throws IOException {
        root = root.toRealPath();
        Path preservedWorld = preserved == null ? null : preserved.getCanonicalFile().toPath();
        Path staging = Files.createTempDirectory(root, ".su-restore-");
        List<Path> targets = new ArrayList<>();
        List<Path> installed = new ArrayList<>();
        List<Path> moved = new ArrayList<>();
        List<Path> createdDirectories = new ArrayList<>();
        boolean recovered = true;
        try {
            // Read and validate the complete archive before modifying any destination.
            try (ZipFile zip = new ZipFile(archive)) {
                Set<Path> seen = new HashSet<>();
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName().replace('\\', '/');
                    if (name.startsWith("/") || name.contains(":") || name.matches("(^|.*/)\\.\\.(/.*|$)")) {
                        throw new IOException("Unsafe backup entry: " + name);
                    }
                    Path relative = restorePath(name, legacy, zip.getComment());
                    Path target = root.resolve(relative);
                    Path canonical = target.toFile().getCanonicalFile().toPath();
                    // The preserved world is the caller's only intact copy; no include pattern may write into it.
                    if (relative.toString().isEmpty() || !canonical.startsWith(root)
                            || target.startsWith(staging)
                            || (preservedWorld != null && canonical.startsWith(preservedWorld))) {
                        throw new IOException("Unsafe backup entry: " + name);
                    }
                    if (entry.isDirectory()) continue;
                    if (!includeGlobal) {
                        String worldName = zip.getComment();
                        boolean worldFile = worldName != null
                                && relative.startsWith(Paths.get("saves", worldName).normalize());
                        if (!worldFile && isGlobal(relative)) continue;
                        if (worldName != null && !isAllowedTarget(relative, worldName)) {
                            serverutils.ServerUtilities.LOGGER
                                    .warn("Skipping unconfigured entry during world-only restore: {}", name);
                            continue;
                        }
                    }
                    if (!seen.add(relative)) throw new IOException("Duplicate backup entry: " + name);
                    Path copy = staging.resolve("new").resolve(relative);
                    Files.createDirectories(copy.getParent());
                    CRC32 checksum = new CRC32();
                    long size;
                    try (InputStream in = new CheckedInputStream(zip.getInputStream(entry), checksum)) {
                        size = Files.copy(in, copy);
                    }
                    if (size != entry.getSize() || checksum.getValue() != entry.getCrc()) {
                        throw new IOException("Backup checksum or size mismatch: " + name);
                    }
                    targets.add(relative);
                }
            }
            for (Path relative : targets) {
                Path target = root.resolve(relative);
                createDirectories(target.getParent(), createdDirectories);
                if (Files.exists(target)) {
                    if (!Files.isRegularFile(target))
                        throw new IOException("Restore target is not a file: " + relative);
                    Path old = staging.resolve("old").resolve(relative);
                    Files.createDirectories(old.getParent());
                    Files.move(target, old);
                    moved.add(relative);
                }
                Files.move(staging.resolve("new").resolve(relative), target);
                installed.add(relative);
            }
            Path originals = staging.resolve("old");
            if (Files.exists(originals)) {
                Path recoveryRoot = root.resolve("backups_before_restore");
                Files.createDirectories(recoveryRoot);
                Path recovery = Files.createTempDirectory(recoveryRoot, "restore-");
                // The final move preserves replaced global files, including automatically included ranks.
                Files.move(originals, recovery.resolve("files"));
            }
        } catch (IOException | RuntimeException failure) {
            Collections.reverse(installed);
            for (Path relative : installed) {
                try {
                    Files.delete(root.resolve(relative));
                } catch (IOException ex) {
                    failure.addSuppressed(ex);
                    recovered = false;
                }
            }
            // A directory left where a displaced original was a file would block its restore below.
            Collections.reverse(createdDirectories);
            for (Path directory : createdDirectories) {
                try {
                    Files.deleteIfExists(directory);
                } catch (IOException ex) {
                    failure.addSuppressed(ex);
                }
            }
            Collections.reverse(moved);
            for (Path relative : moved) {
                try {
                    Files.move(staging.resolve("old").resolve(relative), root.resolve(relative));
                } catch (IOException ex) {
                    failure.addSuppressed(ex);
                    recovered = false;
                }
            }
            if (!recovered) failure.addSuppressed(new IOException("Recovery files retained in " + staging));
            throw failure;
        } finally {
            if (recovered) FileUtils.delete(staging.toFile());
        }
    }

    private static void createDirectories(Path directory, List<Path> created) throws IOException {
        if (directory == null || Files.isDirectory(directory)) return;
        createDirectories(directory.getParent(), created);
        Files.createDirectory(directory);
        created.add(directory);
    }

    private static boolean isGlobal(Path relative) {
        if (isRankFile(relative)) return true;
        for (String pattern : backups.additional_backup_files) {
            if (!pattern.contains("$WORLDNAME") && FileUtils.matchesBackupPath(relative, pattern)) return true;
        }
        return false;
    }
}
