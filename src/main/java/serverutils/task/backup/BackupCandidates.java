package serverutils.task.backup;

import static serverutils.ServerUtilities.LOGGER;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
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
import serverutils.lib.util.compression.ICompress;

@RequiredArgsConstructor
public class BackupCandidates {

    public static final String OVERWORLD = "overworld";

    private final List<WorldFile> additionalFiles = new ArrayList<>();
    private final Map<String, List<WorldFile>> filesByDimension = new HashMap<>();

    /** Real paths of the directories currently being scanned, from the world root down to the current one. */
    private final Set<Path> ancestors = new HashSet<>();
    /** Absolute paths of every candidate, so the same file is never introduced twice. */
    private final Set<Path> known = new HashSet<>();
    /** Candidates that are links to a file, not files of their own. The scan already knows, so nothing re-checks. */
    private final Set<File> links = new HashSet<>();

    private final boolean relativeToWorld;

    private File scanRoot;

    public static BackupCandidates scanWorld(File dir, File backups, boolean relativeToWorld) throws IOException {
        if (!dir.exists()) {
            throw new IllegalArgumentException("World does not exist: " + dir);
        }
        if (!new File(dir, "level.dat").exists()) {
            throw new IllegalArgumentException("World is invalid: " + dir);
        }
        List<Path> storage = FileUtils.resolveRealPaths(backups.toPath(), BackupRoutine.BACKUP_TEMP_FOLDER.toPath());
        if (FileUtils.isInsideAny(dir.toPath().toRealPath(), storage)) {
            throw new IOException("Backup and temporary storage must not contain the world directory: " + dir);
        }
        BackupCandidates candidates = new BackupCandidates(relativeToWorld);
        candidates.scanRoot = dir;
        candidates.scan(dir, dir, OVERWORLD, storage);
        return candidates;
    }

    public boolean isLink(WorldFile file) {
        return links.contains(file.backing());
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
        if (patterns.isEmpty()) return;
        List<Predicate<Path>> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            matchers.add(FileUtils.exactBackupMatcher(pattern.replace("$WORLDNAME", worldName)));
        }

        Path runFolder = Paths.get("").toAbsolutePath();
        Path levelDat = scanRoot == null ? null : scanRoot.toPath().resolve("level.dat").toAbsolutePath().normalize();
        remove(file -> {
            if (file.name().equals(ICompress.BACKUP_METADATA_ENTRY)) return true;
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

    private void scan(File directory, File dimensionRoot, String dimension, List<Path> storage) throws IOException {
        Path real = directory.toPath().toRealPath();
        if (FileUtils.isInsideAny(real, storage)) return;

        if (!ancestors.add(real)) return;

        try {
            List<WorldFile> files = filesByDimension.computeIfAbsent(dimension, k -> new ArrayList<>());
            boolean isSaveRoot = directory.equals(dimensionRoot) && dimension.equals(OVERWORLD);

            Files.walkFileTree(real, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes listed) throws IOException {
                    File child = new File(directory, path.getFileName().toString());
                    // Windows junctions may be reported as other reparse points rather than symbolic links.
                    boolean link = listed.isSymbolicLink() || listed.isOther();
                    BasicFileAttributes attributes = listed;
                    if (link) {
                        try {
                            attributes = Files.readAttributes(path, BasicFileAttributes.class);
                        } catch (IOException broken) {
                            return FileVisitResult.CONTINUE;
                        }
                    }

                    if (attributes.isDirectory()) {
                        if (isSaveRoot && FileUtils.isDimensionFolder(child)) {
                            scan(child, child, child.getName(), storage);
                        } else {
                            scan(child, dimensionRoot, dimension, storage);
                        }
                    } else if (attributes.isRegularFile()) {
                        // Only a link can leave this directory, which is already known to be outside backup storage.
                        if (link && FileUtils.isInsideAny(path.toRealPath(), storage)) return FileVisitResult.CONTINUE;
                        WorldFile file = WorldFile
                                .from(dimension, dimensionRoot, relativeToWorld ? scanRoot : dimensionRoot, child);
                        if (known.add(child.toPath().toAbsolutePath().normalize())) {
                            files.add(file);
                            if (link) links.add(child);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException failure) throws IOException {
                    if (path.equals(real)) throw new IOException("Cannot list backup directory: " + directory, failure);
                    // Removed since it was listed
                    if (failure instanceof NoSuchFileException) return FileVisitResult.CONTINUE;
                    throw failure;
                }
            });
        } finally {
            ancestors.remove(real);
        }
    }
}
