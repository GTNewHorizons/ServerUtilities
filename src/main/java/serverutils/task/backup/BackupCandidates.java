package serverutils.task.backup;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import serverutils.lib.util.FileUtils;

public class BackupCandidates {
    public static final String OVERWORLD = "overworld";

    private final Map<String, List<WorldFile>> filesByDimension = new HashMap<>();
    private final Set<String> scanned = new HashSet<>();

    private BackupCandidates() {
    }

    public static BackupCandidates scanWorld(File folder) {
        if (!new File(folder, "level.dat").exists()) {
            throw new IllegalArgumentException("Not a world folder");
        }
        BackupCandidates candidates = new BackupCandidates();
        candidates.scan(folder, folder, OVERWORLD);
        return candidates;
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
        for (List<WorldFile> files : filesByDimension.values()) {
            files.removeIf(file -> predicate.test(file.backing()));
        }
    }

    private void scan(File directory, File dimensionRoot, String dimension) {
        // protect against infinite loops via symlinks
        if (!scanned.add(directory.getAbsolutePath())) return;

        List<WorldFile> files = filesByDimension.computeIfAbsent(dimension, k -> new ArrayList<>());
        File[] children = directory.listFiles();
        if (children == null) return;

        boolean isSaveRoot = directory.equals(dimensionRoot) && dimension.equals(OVERWORLD);

        for (File child : children) {
            if (!child.isDirectory()) {
                files.add(WorldFile.from(dimensionRoot, child));
            } else if (isSaveRoot && FileUtils.isDimensionFolder(child)) {
                scan(child, child, child.getName());
            } else {
                scan(child, dimensionRoot, dimension);
            }
        }
    }
}