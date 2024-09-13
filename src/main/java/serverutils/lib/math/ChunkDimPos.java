package serverutils.lib.math;

import net.minecraft.entity.Entity;
import net.minecraft.world.ChunkCoordIntPair;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

public final class ChunkDimPos {

    public int posX, posZ, dim;

    public ChunkDimPos() {}

    public ChunkDimPos(int x, int z, int d) {
        posX = x;
        posZ = z;
        dim = d;
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

    public ChunkDimPos set(int x, int z, int dim) {
        this.posX = x;
        this.posZ = z;
        this.dim = dim;
        return this;
    }

    public ChunkDimPos set(long packed, int dim) {
        return set(CoordinatePacker.unpackX(packed), CoordinatePacker.unpackZ(packed), dim);
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

    public boolean equalsChunkDimPos(ChunkDimPos p) {
        return p == this || (p.dim == dim && p.posX == posX && p.posZ == posZ);
    }

    public String toString() {
        return "[" + dim + '@' + posX + ',' + posZ + ']';
    }

    public int hashCode() {
        return 31 * (31 * posX + posZ) + dim;
    }

    public ChunkCoordIntPair getChunkPos() {
        return new ChunkCoordIntPair(posX, posZ);
    }

    public int getBlockX() {
        return (posX << 4) + 8;
    }

    public int getBlockZ() {
        return (posZ << 4) + 8;
    }

    public BlockDimPos getBlockPos(int y) {
        return new BlockDimPos(getBlockX(), y, getBlockZ(), dim);
    }
}
