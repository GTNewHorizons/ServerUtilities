package serverutils.lib.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.storage.RegionFile;

import lombok.Getter;
import serverutils.lib.math.ChunkDimPos;
import serverutils.task.backup.WorldFile;

public class WorldRegion implements AutoCloseable {

    @Getter
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

        AtomicInteger saved = new AtomicInteger();
        for (ChunkDimPos pos : positions) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            int chunkX = pos.posX & 31;
            int chunkZ = pos.posZ & 31;

            read(chunkX, chunkZ, nbt -> {
                other.write(chunkX, chunkZ, () -> nbt);
                saved.getAndIncrement();
            });
        }

        return saved.get();
    }

    public void read(int chunkX, int chunkZ, Reader consumer) throws IOException {
        try (DataInputStream in = regionFile.getChunkDataInputStream(chunkX, chunkZ)) {
            if (in == null) return;
            NBTTagCompound tag = CompressedStreamTools.read(in);
            consumer.accept(tag);
        }
    }

    public void write(int chunkX, int chunkZ, Modifier mod) throws IOException {
        try (DataOutputStream out = regionFile.getChunkDataOutputStream(chunkX, chunkZ)) {
            CompressedStreamTools.write(mod.get(), out);
        }
    }

    @Override
    public void close() throws IOException {
        regionFile.close();
    }

    @FunctionalInterface
    public interface Modifier {

        NBTTagCompound get() throws IOException;
    }

    @FunctionalInterface
    public interface Reader {

        void accept(NBTTagCompound tag) throws IOException;
    }
}
