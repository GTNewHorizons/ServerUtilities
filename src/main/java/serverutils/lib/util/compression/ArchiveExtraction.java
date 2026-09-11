package serverutils.lib.util.compression;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import serverutils.lib.util.FileUtils;

final class ArchiveExtraction {

    static void validateRestoreTargets(File archive, String worldName, boolean legacy) throws IOException {
        Path world = Paths.get("saves", worldName).normalize();
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                Path path = restorePath(entry.getName(), legacy, worldName);
                if (path.startsWith(world) || isRankFile(path)) continue;
                boolean additional = false;
                for (String pattern : backups.additional_backup_files) {
                    pattern = pattern.replace("$WORLDNAME", worldName);
                    if (FileSystems.getDefault().getPathMatcher("glob:" + pattern).matches(path)
                            || (!pattern.contains("*") && path.startsWith(Paths.get(pattern)))) {
                        additional = true;
                        break;
                    }
                }
                if (!additional) throw new IOException("Backup contains an unexpected restore target: " + path);
            }
        }
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

    static void extract(File archive, boolean includeGlobal, boolean legacy) throws IOException {
        extract(archive, includeGlobal, legacy, Paths.get("").toAbsolutePath());
    }

    static void extract(File archive, boolean includeGlobal, boolean legacy, Path root) throws IOException {
        root = root.toRealPath();
        Path staging = Files.createTempDirectory(root, ".su-restore-");
        List<Path> targets = new ArrayList<>();
        List<Path> installed = new ArrayList<>();
        List<Path> moved = new ArrayList<>();
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
                    if (relative.toString().isEmpty() || !target.toFile().getCanonicalFile().toPath().startsWith(root)
                            || target.startsWith(staging)) {
                        throw new IOException("Unsafe backup entry: " + name);
                    }
                    if (entry.isDirectory()) continue;
                    if (!includeGlobal && isGlobal(relative)) continue;
                    if (!seen.add(relative)) throw new IOException("Duplicate backup entry: " + name);
                    Path copy = staging.resolve("new").resolve(relative);
                    Files.createDirectories(copy.getParent());
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, copy);
                    }
                    targets.add(relative);
                }
            }
            for (Path relative : targets) {
                Path target = root.resolve(relative);
                Files.createDirectories(target.getParent());
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

    private static boolean isGlobal(Path relative) {
        if (isRankFile(relative)) return true;
        for (String pattern : backups.additional_backup_files) {
            if (!pattern.contains("$WORLDNAME")
                    && FileSystems.getDefault().getPathMatcher("glob:" + pattern).matches(relative))
                return true;
        }
        return false;
    }
}
