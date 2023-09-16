package ftb.lib;

import net.minecraft.util.ChunkCoordinates;

import latmod.lib.LMUtils;
import latmod.lib.MathHelperLM;

/**
 * Created by LatvianModder on 29.01.2016.
 */
public final class BlockDimPos implements Cloneable {

    public final int x, y, z, dim;

    public BlockDimPos(int px, int py, int pz, int d) {
        x = px;
        y = py;
        z = pz;
        dim = d;
    }

    public BlockDimPos(int[] ai) {
        if (ai == null || ai.length < 4) {
            x = 0;
            y = 256;
            z = 0;
            dim = 0;
        } else {
            x = ai[0];
            y = ai[1];
            z = ai[2];
            dim = ai[3];
        }
    }

    public BlockDimPos(ChunkCoordinates pos, int dim) {
        this(pos.posX, pos.posY, pos.posZ, dim);
    }

    public boolean isValid() {
        return y >= 0 && y < 256;
    }

    public int[] toIntArray() {
        return new int[] { x, y, z, dim };
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(x);
        sb.append(',');
        sb.append(y);
        sb.append(',');
        sb.append(z);
        sb.append(',');
        sb.append(LMDimUtils.getDimName(dim));
        sb.append(']');
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        else if (o == this) return true;
        else return equalsPos((BlockDimPos) o);
    }

    public int hashCode() {
        return LMUtils.hashCode(x, y, z, dim);
    }

    public EntityPos toEntityPos() {
        return new EntityPos(x + 0.5D, y + 0.5D, z + 0.5D, dim);
    }

    public BlockDimPos copy() {
        return new BlockDimPos(x, y, z, dim);
    }

    public ChunkCoordinates toBlockPos() {
        return new ChunkCoordinates(x, y, z);
    }

    public int chunkX() {
        return MathHelperLM.chunk(x);
    }

    public int chunkY() {
        return MathHelperLM.chunk(y);
    }

    public int chunkZ() {
        return MathHelperLM.chunk(z);
    }

    public boolean equalsPos(BlockDimPos p) {
        if (p == null) return false;
        else if (p == this) return true;
        else return p.dim == dim && p.x == x && p.y == y && p.z == z;
    }
}
