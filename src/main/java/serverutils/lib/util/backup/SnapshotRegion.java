package serverutils.lib.util.backup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

import org.apache.commons.codec.binary.Hex;

public final class SnapshotRegion {

    public final String name;
    public final long mtime;
    public final long size;

    private final String hash;
    private final File manifestFile;
    private byte[] bytes;
    private NavigableMap<Integer, ChunkBlob.Metadata> chunks;

    private SnapshotRegion(String name, long mtime, long size, String hash, byte[] bytes, File manifestFile,
            NavigableMap<Integer, ChunkBlob.Metadata> chunks) {
        this.name = name;
        this.mtime = mtime;
        this.size = size;
        this.hash = hash;
        this.bytes = bytes;
        this.manifestFile = manifestFile;
        this.chunks = chunks;
    }

    public static String nameOf(File regionFile) {
        return regionFile.getName().replace(".mca", "");
    }

    public static SnapshotRegion create(String name, long mtime, long size,
            NavigableMap<Integer, ChunkBlob.Metadata> chunks) {
        byte[] bytes = RegionManifest.encode(chunks.values());
        return new SnapshotRegion(name, mtime, size, Hex.encodeHexString(Hasher.hash(bytes)), bytes, null, chunks);
    }

    public static SnapshotRegion stored(String name, ManifestDescriptor descriptor, File manifestFile) {
        return new SnapshotRegion(
                name,
                descriptor.mtime,
                descriptor.size,
                descriptor.manifest,
                null,
                manifestFile,
                null);
    }

    public String getHash() {
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
                bytes = Files.readAllBytes(manifestFile.toPath());
            } catch (IOException e) {
                throw new SnapshotException("Failed to read region manifest " + manifestFile, e);
            }
        }
        return bytes;
    }

    private NavigableMap<Integer, ChunkBlob.Metadata> chunks() {
        if (chunks == null) {
            try {
                chunks = RegionManifest.decode(toBytes());
            } catch (IOException e) {
                throw new SnapshotException("Corrupt region manifest " + manifestFile, e);
            }
        }
        return chunks;
    }
}
