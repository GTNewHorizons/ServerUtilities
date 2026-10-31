package serverutils.task.backup;

import static serverutils.ServerUtilities.LOGGER;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import serverutils.lib.util.FileUtils;

@RequiredArgsConstructor
public class BackupCandidates {

    public static final String OVERWORLD = "overworld";

    private final List<WorldFile> additionalFiles = new ArrayList<>();
    private final Map<String, List<WorldFile>> filesByDimension = new HashMap<>();

    /** Real paths of the directories currently being scanned, from the world root down to the current one. */
    private final Set<Path> ancestors = new HashSet<>();
    /** Absolute paths of every candidate, so the same file is never introduced twice. */
    private final Set<Path> known = new HashSet<>();

    private final File backups;
    private final boolean relativeToWorld;

    private File scanRoot;

    public static BackupCandidates scanWorld(File dir, File backups, boolean relativeToWorld) throws IOException {
        if (!dir.exists()) {
            throw new IllegalArgumentException("World does not exist: " + dir);
        }
        if (!new File(dir, "level.dat").exists()) {
            throw new IllegalArgumentException("World is invalid: " + dir);
        }
        if (isBackupStorage(backups, dir.toPath().toRealPath())) {
            throw new IOException("Backup and temporary storage must not contain the world directory: " + dir);
        }
        BackupCandidates candidates = new BackupCandidates(backups, relativeToWorld);
        candidates.scanRoot = dir;
        candidates.scan(dir, dir, OVERWORLD);
        return candidates;
    }

    /**
     * Introduces a file that is not part of the world scan (or ignores it if it is already a candidate). The entry is
     * named relative to the run folder.
     */
    public void addFile(File file) {
        if (!known.add(file.toPath().toAbsolutePath().normalize())) return;
        additionalFiles.add(new WorldFile(FileUtils.getRelativePath(file), file, WorldFile.Kind.MISC));
    }

    public void addFiles(Collection<File> files) {
        for (File file : files) addFile(file);
    }

    /** Returns the **FILES** (NO DIRECTORIES) for all files in a dimension **/
    public List<WorldFile> getFiles(String dimension) {
        List<WorldFile> files = filesByDimension.get(dimension);
        if (files != null) return Collections.unmodifiableList(files);

        if (dimension.equals(OVERWORLD) || dimension.startsWith("DIM")) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(filesByDimension.getOrDefault("DIM" + dimension, Collections.emptyList()));
    }

    public int count() {
        return filesByDimension.values().stream().mapToInt(List::size).sum() + additionalFiles.size();
    }

    public List<WorldFile> getFiles() {
        return getFiles(worldFile -> true);
    }

    public List<WorldFile> getFiles(Predicate<WorldFile> predicate) {
        return Stream.concat(filesByDimension.values().stream().flatMap(Collection::stream), additionalFiles.stream())
                .filter(predicate).collect(Collectors.toList());
    }

    public Map<String, List<WorldFile>> getFilesByDimension() {
        Map<String, List<WorldFile>> unmodifiableMap = new HashMap<>();
        for (Map.Entry<String, List<WorldFile>> entry : filesByDimension.entrySet()) {
            unmodifiableMap.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(unmodifiableMap);
    }

    public List<String> dimensions() {
        return new ArrayList<>(filesByDimension.keySet());
    }

    public List<WorldFile> getRegionFiles(String dimension) {
        return filter(dimension, WorldFile.Kind.REGION);
    }

    public List<WorldFile> getMiscFiles(String dimension) {
        return filter(dimension, WorldFile.Kind.MISC);
    }

    private List<WorldFile> filter(String dimension, WorldFile.Kind kind) {
        List<WorldFile> result = new ArrayList<>();
        for (WorldFile file : getFiles(dimension)) {
            if (file.kind() == kind) result.add(file);
        }
        return result;
    }

    public void remove(Predicate<WorldFile> predicate) {
        additionalFiles.removeIf(predicate);
        for (List<WorldFile> files : filesByDimension.values()) {
            files.removeIf(predicate);
        }
    }

    public void keep(Predicate<WorldFile> predicate) {
        additionalFiles.removeIf(f -> !predicate.test(f));
        for (List<WorldFile> files : filesByDimension.values()) {
            files.removeIf(f -> !predicate.test(f));
        }
    }

    /**
     * Removes every non-region candidate matched by any of {@code patterns}. {@code $WORLDNAME} is replaced with
     * {@code worldName}; a pattern without wildcards matches only that exact path, otherwise it is a glob. Each pattern
     * is tried against the file's path relative to the run folder and against its absolute path. Empty patterns are
     * ignored. Region files are never removed: which regions are backed up is decided by the chunk selection.
     */
    public void removeMatching(Collection<String> patterns, String worldName) {
        List<Predicate<Path>> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            String normalized = FileUtils.normalizeBackupPattern(pattern.replace("$WORLDNAME", worldName));
            if (normalized.isEmpty()) continue;
            if (FileUtils.indexOfWildcard(normalized) < 0) {
                Path literal = Paths.get(normalized).normalize();
                matchers.add(literal::equals);
            } else {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + normalized)::matches);
            }
        }
        if (matchers.isEmpty()) return;

        Path runFolder = Paths.get("").toAbsolutePath();
        Path levelDat = scanRoot == null ? null : scanRoot.toPath().resolve("level.dat").toAbsolutePath().normalize();
        remove(file -> {
            if (file.isRegionFile()) return false;
            Path absolute = file.backing().toPath().toAbsolutePath().normalize();
            Path relative = runFolder.relativize(absolute);
            for (Predicate<Path> matcher : matchers) {
                if (matcher.test(relative) || matcher.test(absolute)) {
                    // A backup without level.dat cannot be restored
                    if (!absolute.equals(levelDat)) return true;
                    LOGGER.warn("Ignoring backup exclusion for {}, it is required to restore the world", relative);
                    return false;
                }
            }
            return false;
        });
    }

    /** Compares real paths, so backup output is recognized however it is reached (other spelling, link, etc.). */
    private static boolean isBackupStorage(File backups, Path real) throws IOException {
        return real.startsWith(FileUtils.resolveRealPath(backups.toPath()))
                || real.startsWith(FileUtils.resolveRealPath(BackupRoutine.BACKUP_TEMP_FOLDER.toPath()));
    }

    private void scan(File directory, File dimensionRoot, String dimension) throws IOException {
        Path real = directory.toPath().toRealPath();
        if (isBackupStorage(backups, real)) return;

        if (!ancestors.add(real)) return;

        try {
            List<WorldFile> files = filesByDimension.computeIfAbsent(dimension, k -> new ArrayList<>());
            File[] children = directory.listFiles();
            if (children == null) throw new IOException("Cannot list backup directory: " + directory);

            boolean isSaveRoot = directory.equals(dimensionRoot) && dimension.equals(OVERWORLD);

            for (File child : children) {
                // exists() follows links, so this ignores broken ones.
                if (!child.exists()) continue;
                if (!child.isDirectory()) {
                    // The directory is outside backup storage, skip
                    if (Files.isSymbolicLink(child.toPath()) && isBackupStorage(backups, child.toPath().toRealPath()))
                        continue;
                    WorldFile file = WorldFile
                            .from(dimension, dimensionRoot, relativeToWorld ? scanRoot : dimensionRoot, child);
                    if (known.add(child.toPath().toAbsolutePath().normalize())) files.add(file);
                } else if (isSaveRoot && FileUtils.isDimensionFolder(child)) {
                    scan(child, child, child.getName());
                } else {
                    scan(child, dimensionRoot, dimension);
                }
            }
        } finally {
            ancestors.remove(real);
        }
    }
}
