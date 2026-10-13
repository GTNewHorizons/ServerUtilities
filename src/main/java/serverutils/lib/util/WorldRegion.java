package serverutils.lib.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Objects;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.storage.RegionFile;

import serverutils.lib.math.ChunkDimPos;
import serverutils.task.backup.WorldFile;

public class WorldRegion implements AutoCloseable {

    private final File source;
    public final RegionFile regionFile;
    public final String dimension;

    public WorldRegion(File file, String dimension) {
        if (!file.getName().endsWith(".mca")) {
            throw new IllegalArgumentException("File is not a region file");
        }
        this.source = file;
        this.regionFile = new RegionFile(file);
        this.dimension = dimension;
    }

    public WorldRegion(WorldFile source) {
        this(source.backing(), Objects.requireNonNull(source.dimension));
    }

    public String name() {
        return source.getName().replace(".mca", "");
    }

    public int copyTo(WorldRegion other, List<ChunkDimPos> positions) throws IOException {
        if (!Objects.equals(dimension, other.dimension)) {
            throw new IllegalArgumentException("Dimension mismatch");
        }

        int saved = 0;
        for (ChunkDimPos pos : positions) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            int chunkX = pos.posX & 31;
            int chunkZ = pos.posZ & 31;
            try (DataInputStream in = regionFile.getChunkDataInputStream(chunkX, chunkZ)) {
                if (in == null) continue;
                NBTTagCompound tag = CompressedStreamTools.read(in);
                try (DataOutputStream out = other.regionFile.getChunkDataOutputStream(chunkX, chunkZ)) {
                    CompressedStreamTools.write(tag, out);
                }
                saved++;
            }
        }

        return saved;
    }

    @Override
    public void close() throws IOException {
        regionFile.close();
    }
}
