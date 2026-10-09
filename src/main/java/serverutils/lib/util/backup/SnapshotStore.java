package serverutils.lib.util.backup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import serverutils.ServerUtilities;

public class SnapshotStore {

    private static final String ID_FILE = "serverutilities/snapshot_id";
    private final File root;

    public final File packsDirectory;
    public final File manifestsDirectory;

    private final ManifestStore manifestStore;

    private SnapshotStore(File root) throws IOException {
        this.root = root;
        packsDirectory = new File(root, "objects/packs");
        manifestsDirectory = new File(root, "objects/manifests");

        ensureDirectory(packsDirectory);
        ensureDirectory(manifestsDirectory);
        manifestStore = new ManifestStore(manifestsDirectory);
    }

    public static SnapshotStore load(File root) throws IOException {
        return new SnapshotStore(root);
    }

    public ManifestStore manifests() {
        return manifestStore;
    }

    public static File backupDirectoryFor(File worldDirectory, File backupsRoot) {
        String name = worldDirectory.getName().replaceAll("[^A-Za-z0-9._-]", "_");
        return new File(backupsRoot, name + "-" + getOrCreateWorldId(worldDirectory));
    }

    public Snapshot findLatest() {
        reloadManifests();
        List<SnapshotJson> files = listJsonFiles();
        if (files.isEmpty()) return null;
        return Snapshot.fromJson(manifestStore, files.get(files.size() - 1));
    }

    public Snapshot findOldest() {
        reloadManifests();
        List<SnapshotJson> files = listJsonFiles();
        if (files.isEmpty()) return null;
        return Snapshot.fromJson(manifestStore, files.get(0));
    }

    public List<Snapshot> listAll() {
        reloadManifests();
        return listJsonFiles().stream().map(json -> Snapshot.fromJson(manifestStore, json, true))
                .collect(Collectors.toList());
    }

    public void reloadManifests() {
        try {
            manifestStore.reload();
        } catch (IOException e) {
            throw new SnapshotException("Failed to read manifest packs", e);
        }
    }

    public int count() {
        return listJsonFiles().size();
    }

    public void write(Snapshot snapshot) {
        File file = new File(root, snapshot.getName() + ".json");
        try {
            SnapshotJson.write(file, snapshot.toJson());
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

    private List<SnapshotJson> listJsonFiles() {
        File[] files = root.listFiles(f -> f.isFile() && Snapshot.FILE_PATTERN.matcher(f.getName()).matches());
        if (files == null || files.length == 0) return Collections.emptyList();

        Arrays.sort(files, Comparator.comparing(File::getName));
        List<SnapshotJson> json = new ArrayList<>(files.length);

        for (File file : files) {
            try {
                json.add(SnapshotJson.read(file));
            } catch (IOException e) {
                ServerUtilities.LOGGER.error("Failed to read snapshot file {}", file, e);
            }
        }

        return json;
    }

    // TODO: I'm not too familiar with all of GTNH yet, im sure there's a uuid generated per world but
    // I'm not sure how to access it so I'm just using this for now, someone please correct (though not critical)
    private static UUID getOrCreateWorldId(File worldDirectory) {
        File idFile = new File(worldDirectory, ID_FILE);

        if (idFile.isFile()) {
            try {
                return UUID.fromString(new String(Files.readAllBytes(idFile.toPath()), StandardCharsets.UTF_8).trim());
            } catch (IllegalArgumentException | IOException e) {
                ServerUtilities.LOGGER.warn("Invalid snapshot id in {}, generating a new one", idFile, e);
            }
        }

        UUID id = UUID.randomUUID();
        try {
            File parent = idFile.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Could not create " + parent);
            Files.write(idFile.toPath(), id.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SnapshotException("Failed to save snapshot id " + idFile, e);
        }
        return id;
    }

    private static void ensureDirectory(File directory) {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new SnapshotException("Failed to create directory " + directory);
        }
    }
}
