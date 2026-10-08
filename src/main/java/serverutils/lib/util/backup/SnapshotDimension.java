package serverutils.lib.util.backup;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.apache.commons.codec.binary.Hex;

public class SnapshotDimension {
    public final int id;

    private final Map<String, Region> regions = new HashMap<>();
    public List<File> regionsFiles;

    public SnapshotDimension(int id, List<File> regionsFiles) {
        this.id = id;
        this.regionsFiles = regionsFiles;
    }

    public List<Region> getRegions() {
        return new ArrayList<>(regions.values());
    }

    public Region getRegion(String name) {
        return regions.get(name.replace(".mca", ""));
    }

    public Region registerRegion(File regionFile, List<ChunkBlob> blobs) {
        TreeMap<Integer, ChunkBlob.Metadata> metadata = new TreeMap<>();
        blobs.forEach(b -> {
            ChunkBlob.Metadata chunkMeta = b.metadata();
            metadata.put(chunkMeta.index(), chunkMeta);
        });

        long mtime = regionFile.lastModified();
        long size = regionFile.length();

        Region region = new Region(regionFile.getName().replace(".mca", ""), metadata, mtime, size);
        return registerRegion(region);
    }

    public Region registerRegion(Region region) {
        regions.put(region.name, region);
        return region;
    }

    public void populateFromJson(SnapshotJson.Dimension dimensionJson, File root) {
        for (Map.Entry<String, ManifestDescriptor> entry : dimensionJson.kinds.regions.entrySet()) {
            String region = entry.getKey();
            ManifestDescriptor descriptor = entry.getValue();

            File manifestFile = root.toPath().resolve("objects/manifests/" + descriptor.manifest).toFile();
            if (!manifestFile.exists()) {
                continue; // TODO log an error or something
            }

            regions.put(region, new Region(region, descriptor, manifestFile));
        }
    }

    public static class Region {
        public static final String MAGIC = "MREG";
        private final TreeMap<Integer, ChunkBlob.Metadata> chunksMetadata;
        private File manifestFile;
        private boolean loaded;

        private String hash;
        private byte[] bytes;

        public final String name;
        public long mtime;
        public long size;

        public Region(String name, TreeMap<Integer, ChunkBlob.Metadata> chunks, long mtime, long size) {
            this.name = name;
            this.chunksMetadata = chunks;
            this.mtime = mtime;
            this.size = size;
        }

        public Region(String name, ManifestDescriptor descriptor, File manifestFile) {
            this.name = name;
            this.mtime = descriptor.mtime;
            this.size = descriptor.size;
            this.hash = descriptor.manifest; // already known, no need to reread the manifest to rehash it
            this.chunksMetadata = new TreeMap<>();
            this.manifestFile = manifestFile;
        }

        public String getHash() {
            if (hash != null) return hash;

            try {
                hash = Hex.encodeHexString(Hasher.hash(toBytes()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return hash;
        }

        public List<ChunkBlob.Metadata> getChunks() {
            return new ArrayList<>(chunksMetadata.values());
        }

        public ChunkBlob.Metadata getChunk(int id) {
            ChunkBlob.Metadata metadata = chunksMetadata.get(id);
            if (loaded) return metadata;

            if (metadata == null && manifestFile != null) readManifestFile();
            return chunksMetadata.get(id);
        }

        public byte[] toBytes() throws IOException {
            if (bytes != null) return bytes;
            if (manifestFile != null) readManifestFile();

            ByteArrayOutputStream bytes = new ByteArrayOutputStream(6 + chunksMetadata.size() * 38);
            DataOutputStream out = new DataOutputStream(bytes);

            out.writeBytes(MAGIC);
            out.writeShort(chunksMetadata.size());

            for (ChunkBlob.Metadata metadata : chunksMetadata.values()) {
                out.writeShort(metadata.index());
                out.writeInt((int)metadata.timestamp());
                out.write(metadata.hash());
            }

            this.bytes = bytes.toByteArray();
            return this.bytes;
        }

        public void readManifestFile() {
            if (loaded) return;
            Objects.requireNonNull(manifestFile);

            try {
                bytes = Files.readAllBytes(manifestFile.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String magic = new String(Arrays.copyOf(bytes, 4), StandardCharsets.UTF_8);
            // corrupt
            if (!MAGIC.equals(magic)) return;

            ByteBuffer buffer = ByteBuffer.wrap(bytes, 4, bytes.length - 4);

            short chunkCount = buffer.getShort();
            int i = 0;
            while (i++ < chunkCount) {
                short index = buffer.getShort();
                long timestamp = Integer.toUnsignedLong(buffer.getInt());
                byte[] hash = new byte[32];
                buffer.get(hash);

                chunksMetadata.put((int)index, new ChunkBlob.Metadata(index, timestamp, 0, hash));
            }

            loaded = true;
        }
    }
}
