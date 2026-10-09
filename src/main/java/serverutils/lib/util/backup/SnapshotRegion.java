package serverutils.lib.util.backup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

public final class SnapshotRegion implements Hashable {

    public final String name;
    public final long mtime;
    public final long size;

    private final SHAHash hash;
    private final PackStore store;
    private byte[] bytes;
    private NavigableMap<Integer, ChunkBlob.Metadata> chunks;

    private SnapshotRegion(String name, long mtime, long size, SHAHash hash, byte[] bytes, PackStore store,
            NavigableMap<Integer, ChunkBlob.Metadata> chunks) {
        this.name = name;
        this.mtime = mtime;
        this.size = size;
        this.hash = hash;
        this.bytes = bytes;
        this.store = store;
        this.chunks = chunks;
    }

    public static String nameOf(File regionFile) {
        return regionFile.getName().replace(".mca", "");
    }

    public static SnapshotRegion create(String name, long mtime, long size,
            NavigableMap<Integer, ChunkBlob.Metadata> chunks) {
        byte[] bytes = RegionManifest.encode(chunks.values());
        return new SnapshotRegion(name, mtime, size, SHAHash.compute(bytes), bytes, null, chunks);
    }

    public static SnapshotRegion stored(String name, SHAHash hash, ManifestDescriptor descriptor, PackStore store) {
        return new SnapshotRegion(name, descriptor.mtime, descriptor.size, hash, null, store, null);
    }

    @Override
    public SHAHash hash() {
        return hash;
    }

    public List<ChunkBlob.Metadata> getChunks() {
        return new ArrayList<>(chunks().values());
    }

    public ChunkBlob.Metadata getChunk(int index) {
        return chunks().get(index);
    }

    public byte[] toBytes() {
        if (bytes == null) {
            try {
                bytes = store.readBlob(hash).data();
            } catch (IOException e) {
                throw new SnapshotException("Failed to read region manifest " + hash, e);
            }
        }
        return bytes;
    }

    private NavigableMap<Integer, ChunkBlob.Metadata> chunks() {
        if (chunks == null) {
            try {
                chunks = RegionManifest.decode(toBytes());
            } catch (IOException e) {
                throw new SnapshotException("Corrupt region manifest " + hash, e);
            }
        }
        return chunks;
    }
}
