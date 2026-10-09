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
    public final File filesDirectory;
    public final File manifestsDirectory;

    private final PackStore manifestStore;
    private final PackStore fileStore;
    // there is an entry per stored chunk, so it is only read when something needs chunk data
    private PackStore chunkStore;

    private SnapshotStore(File root) throws IOException {
        this.root = root;
        packsDirectory = new File(root, "objects/packs");
        filesDirectory = new File(root, "objects/files");
        manifestsDirectory = new File(root, "objects/manifests");

        FileUtils.ensureExists(packsDirectory);
        FileUtils.ensureExists(filesDirectory);
        FileUtils.ensureExists(manifestsDirectory);
        manifestStore = new PackStore(manifestsDirectory);
        fileStore = new PackStore(filesDirectory);
    }

    public static SnapshotStore load(File root) throws IOException {
        return new SnapshotStore(root);
    }

    /** Region manifests */
    public PackStore manifests() {
        return manifestStore;
    }

    /** Blobs of the non-region files */
    public PackStore files() {
        return fileStore;
    }

    public synchronized PackStore chunks() {
        if (chunkStore == null) {
            try {
                chunkStore = new PackStore(packsDirectory);
            } catch (IOException e) {
                throw new SnapshotException("Failed to read chunk packs", e);
            }
        }
        return chunkStore;
    }

    public static File backupDirectoryFor(File backupsRoot, File worldDirectory) {
        return new File(backupsRoot, worldDirectory.getName());
    }

    public Snapshot findLatest() {
        reloadManifests();
        List<SnapshotManifest> files = listManifest();
        if (files.isEmpty()) return null;
        return Snapshot.fromJson(this, files.get(files.size() - 1));
    }

    public Snapshot findOldest() {
        reloadManifests();
        List<SnapshotManifest> files = listManifest();
        if (files.isEmpty()) return null;
        return Snapshot.fromJson(this, files.get(0));
    }

    public Snapshot get(SnapshotManifest manifest) {
        return Snapshot.fromJson(this, manifest);
    }

    public List<Snapshot> listAll() {
        reloadManifests();
        return listManifest().stream().map(json -> Snapshot.fromJson(this, json, true)).collect(Collectors.toList());
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

    public synchronized void reloadManifests() {
        try {
            manifestStore.reload();
            fileStore.reload();
        } catch (IOException e) {
            throw new SnapshotException("Failed to read manifest packs", e);
        }

        if (chunkStore != null) {
            chunkStore.close();
            chunkStore = null;
        }
    }

    public synchronized void closeHandles() {
        manifestStore.closeHandles();
        fileStore.closeHandles();
        if (chunkStore != null) chunkStore.closeHandles();
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
