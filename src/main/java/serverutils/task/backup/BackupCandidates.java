package serverutils.task.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
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

import serverutils.lib.util.FileUtils;

public class BackupCandidates {

    public static final String OVERWORLD = "overworld";

    private final List<WorldFile> additionalFiles = new ArrayList<>();
    private final Map<String, List<WorldFile>> filesByDimension = new HashMap<>();

    /** Real paths of the directories currently being scanned, from the world root down to the current one. */
    private final Set<Path> ancestors = new HashSet<>();
    private final boolean relativeToWorld;
    /** Absolute paths of every candidate, so the same file is never introduced twice. */
    private final Set<Path> known = new HashSet<>();

    private File scanRoot;

    private BackupCandidates(boolean relativeToWorld) {
        this.relativeToWorld = relativeToWorld;
    }

    public static BackupCandidates scanWorld(File dir, boolean relativeToWorld) {
        if (!new File(dir, "level.dat").exists()) {
            throw new IllegalArgumentException("Not a world folder");
        }
        BackupCandidates candidates = new BackupCandidates(relativeToWorld);
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

    public void remove(Predicate<File> predicate) {
        additionalFiles.removeIf(file -> predicate.test(file.backing()));
        for (List<WorldFile> files : filesByDimension.values()) {
            files.removeIf(file -> predicate.test(file.backing()));
        }
    }

    /**
     * Removes every candidate matched by any of {@code patterns}, following the same rules as
     * {@link FileUtils#findMatching}. Each pattern is tried against the file's path relative to the run folder and
     * against its absolute path. Empty patterns are ignored.
     */
    public void removeMatching(Collection<String> patterns) {
        List<Predicate<Path>> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            String normalized = FileUtils.normalizeBackupPattern(pattern);
            if (normalized.isEmpty()) continue;
            if (FileUtils.indexOfWildcard(normalized) < 0) {
                Path literal = Paths.get(normalized).normalize();
                matchers.add(path -> path.startsWith(literal));
            } else {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + normalized)::matches);
            }
        }
        if (matchers.isEmpty()) return;

        Path runFolder = Paths.get("").toAbsolutePath();
        remove(file -> {
            Path absolute = file.toPath().toAbsolutePath().normalize();
            Path relative = runFolder.relativize(absolute);
            for (Predicate<Path> matcher : matchers) {
                if (matcher.test(relative) || matcher.test(absolute)) return true;
            }
            return false;
        });
    }

    private void scan(File directory, File dimensionRoot, String dimension) {
        if (BackupRoutine.BACKUP_FOLDER.equals(directory)) return;
        if (BackupRoutine.BACKUP_TEMP_FOLDER.equals(directory)) return;

        Path real;
        try {
            real = directory.toPath().toRealPath();
        } catch (IOException e) {
            return;
        }

        if (!ancestors.add(real)) return;

        try {
            List<WorldFile> files = filesByDimension.computeIfAbsent(dimension, k -> new ArrayList<>());
            File[] children = directory.listFiles();
            if (children == null) return;

            boolean isSaveRoot = directory.equals(dimensionRoot) && dimension.equals(OVERWORLD);

            for (File child : children) {
                // exists() follows links, so this ignores broken ones.
                if (!child.exists()) continue;
                if (!child.isDirectory()) {
                    WorldFile file = relativeToWorld ? WorldFile.from(scanRoot, child)
                            : WorldFile.from(dimensionRoot, child);
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
