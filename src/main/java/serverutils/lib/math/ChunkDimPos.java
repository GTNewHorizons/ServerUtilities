package serverutils.lib.math;

import net.minecraft.entity.Entity;
import net.minecraft.world.ChunkCoordIntPair;

import org.joml.Vector3i;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

public final class ChunkDimPos extends Vector3i {

    public ChunkDimPos(int x, int z, int d) {
        super(x, d, z);
    }

    public ChunkDimPos(ChunkCoordIntPair pos, int d) {
        this(pos.chunkXPos, pos.chunkZPos, d);
    }

    public ChunkDimPos(int posx, int posy, int posz, int d) {
        this(MathUtils.chunk(posx), MathUtils.chunk(posz), d);
    }

    public ChunkDimPos(Entity entity) {
        this(MathUtils.chunk(entity.posX), MathUtils.chunk(entity.posZ), entity.worldObj.provider.dimensionId);
    }

    public ChunkDimPos(long packed) {
        this(CoordinatePacker.unpackX(packed), CoordinatePacker.unpackZ(packed), CoordinatePacker.unpackY(packed));
    }

    public ChunkDimPos() {}

    public ChunkDimPos set(int x, int z, int d) {
        super.set(x, d, z);
        return this;
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof ChunkDimPos) {
            return equalsChunkDimPos((ChunkDimPos) o);
        }
        return false;
    }

    public ChunkDimPos setFromLong(long packed) {
        set(unpackX(packed), unpackDim(packed), unpackZ(packed));
        return this;
    }

    public boolean equalsChunkDimPos(ChunkDimPos p) {
        return p == this || (p.getDim() == getDim() && p.x == x && p.z == z);
    }

    public boolean equalsRegionPos(ChunkDimPos p) {
        return p == this || (p.y == y && (p.x >> 5) == (x >> 5) && (p.z >> 5) == (z >> 5));
    }

    public String toString() {
        return "[" + getDim() + '@' + x + ',' + z + ']';
    }

    public ChunkCoordIntPair getChunkPos() {
        return new ChunkCoordIntPair(x, z);
    }

    public int getBlockX() {
        return (x << 4) + 8;
    }

    public int getBlockZ() {
        return (z << 4) + 8;
    }

    public int getDim() {
        return y;
    }

    public BlockDimPos getBlockPos(int blockY) {
        return new BlockDimPos(getBlockX(), blockY, getBlockZ(), getDim());
    }

    public long toLong() {
        return CoordinatePacker.pack(x, getDim(), z);
    }

    public long toRegionLong() {
        return CoordinatePacker.pack(x >> 5, getDim(), z >> 5);
    }

    public int unpackX(long packed) {
        return CoordinatePacker.unpackX(packed);
    }

    public int unpackZ(long packed) {
        return CoordinatePacker.unpackZ(packed);
    }

    public int unpackDim(long packed) {
        return CoordinatePacker.unpackY(packed);
    }
}
