package serverutils.lib.util.backup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

import serverutils.ServerUtilities;

public final class SnapshotStore {

    private static final String ID_FILE = "serverutilities/snapshot_id";

    private SnapshotStore() {}

    public static File packsDirectory(File store) {
        return new File(store, "objects/packs");
    }

    public static File manifestsDirectory(File store) {
        return new File(store, "objects/manifests");
    }

    /** @return the most recent readable snapshot in the store, or null if there is none */
    public static Snapshot findLatest(File store) {
        File[] files = store.listFiles(f -> f.isFile() && Snapshot.FILE_PATTERN.matcher(f.getName()).matches());
        if (files == null || files.length == 0) return null;

        Arrays.sort(files, Comparator.comparing(File::getName).reversed());

        for (File file : files) {
            try {
                return Snapshot.fromJson(manifestsDirectory(store), SnapshotJson.read(file));
            } catch (Exception e) {
                // an interrupted run can leave a corrupt file, fall back to the one before it
                ServerUtilities.LOGGER.error("Failed to read snapshot file {}", file, e);
            }
        }
        return null;
    }

    public static void write(File store, Snapshot snapshot) {
        File file = new File(store, snapshot.getName() + ".json");
        try {
            SnapshotJson.write(file, snapshot.toJson());
        } catch (IOException e) {
            throw new SnapshotException("Failed to write snapshot file " + file, e);
        }
    }

    public static File backupDirectoryFor(File worldDirectory, File backupsRoot) {
        String name = worldDirectory.getName().replaceAll("[^A-Za-z0-9._-]", "_");
        return new File(backupsRoot, name + "-" + getOrCreateWorldId(worldDirectory));
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
}
