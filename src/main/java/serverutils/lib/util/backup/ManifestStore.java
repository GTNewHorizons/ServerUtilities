package serverutils.lib.util.backup;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.bsideup.jabel.Desugar;

public final class ManifestStore implements Closeable {

    private static final int MAX_OPEN_PACKS = 16;

    @Desugar
    private record Location(File pack, long offset, int length) {}

    private final File directory;
    private final Map<SHAHash, Location> locations = new HashMap<>();
    private final Map<File, RandomAccessFile> handles = new LinkedHashMap<>(MAX_OPEN_PACKS, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<File, RandomAccessFile> eldest) {
            if (size() <= MAX_OPEN_PACKS) return false;
            closeQuietly(eldest.getValue());
            return true;
        }
    };

    public ManifestStore(File directory) throws IOException {
        this.directory = directory;
        reload();
    }

    /** Re-reads the pack indexes from disk and closes every cached handle. */
    public synchronized void reload() throws IOException {
        Map<SHAHash, Location> loaded = new HashMap<>();

        File[] indexes = directory.listFiles((dir, name) -> name.endsWith(PackIndex.EXTENSION));
        if (indexes != null) {
            for (File index : indexes) {
                String name = index.getName();
                File pack = new File(
                        directory,
                        name.substring(0, name.length() - PackIndex.EXTENSION.length()) + ".pack");
                if (!pack.isFile()) continue; // interrupted before the pack was moved into place

                for (PackIndex.Entry entry : PackIndex.read(index)) {
                    loaded.put(entry.hash(), new Location(pack, entry.offset(), entry.length()));
                }
            }
        }

        closeHandles();
        locations.clear();
        locations.putAll(loaded);
    }

    public synchronized boolean contains(SHAHash hash) {
        return locations.containsKey(hash);
    }

    public synchronized byte[] read(SHAHash hash) throws IOException {
        Location location = locations.get(hash);
        if (location == null) throw new IOException("Region manifest " + hash + " is not in any pack");

        byte[] bytes = new byte[location.length()];
        RandomAccessFile file = handle(location.pack());
        file.seek(location.offset());
        file.readFully(bytes);
        return bytes;
    }

    /** Closes every cached pack handle, the store stays usable. */
    public synchronized void closeHandles() {
        Iterator<RandomAccessFile> iterator = handles.values().iterator();
        while (iterator.hasNext()) {
            closeQuietly(iterator.next());
            iterator.remove();
        }
    }

    @Override
    public void close() {
        closeHandles();
    }

    private RandomAccessFile handle(File pack) throws IOException {
        RandomAccessFile file = handles.get(pack);
        if (file == null) {
            file = new RandomAccessFile(pack, "r");
            handles.put(pack, file);
        }
        return file;
    }

    private static void closeQuietly(RandomAccessFile file) {
        try {
            file.close();
        } catch (IOException ignored) {}
    }
}
