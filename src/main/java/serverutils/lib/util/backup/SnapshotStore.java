package serverutils.lib.util.backup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import serverutils.ServerUtilities;
import serverutils.lib.util.FileUtils;

public class SnapshotStore {

    private final File root;

    public final File packsDirectory;
    public final File manifestsDirectory;

    private final ManifestStore manifestStore;

    private SnapshotStore(File root) throws IOException {
        this.root = root;
        packsDirectory = new File(root, "objects/packs");
        manifestsDirectory = new File(root, "objects/manifests");

        FileUtils.ensureExists(packsDirectory);
        FileUtils.ensureExists(manifestsDirectory);
        manifestStore = new ManifestStore(manifestsDirectory);
    }

    public static SnapshotStore load(File root) throws IOException {
        return new SnapshotStore(root);
    }

    public ManifestStore manifests() {
        return manifestStore;
    }

    public static File backupDirectoryFor(File backupsRoot, File worldDirectory) {
        return new File(backupsRoot, worldDirectory.getName());
    }

    public Snapshot findLatest() {
        reloadManifests();
        List<SnapshotManifest> files = listManifest();
        if (files.isEmpty()) return null;
        return Snapshot.fromJson(manifestStore, files.get(files.size() - 1));
    }

    public Snapshot findOldest() {
        reloadManifests();
        List<SnapshotManifest> files = listManifest();
        if (files.isEmpty()) return null;
        return Snapshot.fromJson(manifestStore, files.get(0));
    }

    public Snapshot get(SnapshotManifest manifest) {
        return Snapshot.fromJson(manifestStore, manifest);
    }

    public List<Snapshot> listAll() {
        reloadManifests();
        return listManifest().stream().map(json -> Snapshot.fromJson(manifestStore, json, true))
                .collect(Collectors.toList());
    }

    public List<SnapshotManifest> listManifest() {
        File[] files = root.listFiles(f -> f.isFile() && Snapshot.FILE_PATTERN.matcher(f.getName()).matches());
        if (files == null || files.length == 0) return Collections.emptyList();

        Arrays.sort(files, Comparator.comparing(File::getName));
        List<SnapshotManifest> manifest = new ArrayList<>(files.length);

        for (File file : files) {
            try {
                manifest.add(SnapshotManifest.read(file));
            } catch (IOException e) {
                ServerUtilities.LOGGER.error("Failed to read snapshot file {}", file, e);
            }
        }

        return manifest;
    }

    public void reloadManifests() {
        try {
            manifestStore.reload();
        } catch (IOException e) {
            throw new SnapshotException("Failed to read manifest packs", e);
        }
    }

    public int count() {
        return listManifest().size();
    }

    public void write(Snapshot snapshot) {
        File file = new File(root, snapshot.getName() + ".json");
        try {
            SnapshotManifest.write(file, snapshot.toJson());
        } catch (IOException e) {
            throw new SnapshotException("Failed to write snapshot file " + file, e);
        }
    }

    /**
     * Removes the snapshot **json** file from the snapshot store. This does NOT clean up old chunks
     */
    public void removeEntry(Snapshot snapshot) {
        File file = new File(root, snapshot.getName() + ".json");
        if (file.exists() && !file.delete()) {
            throw new SnapshotException("Failed to delete snapshot file " + file);
        }
    }
}
