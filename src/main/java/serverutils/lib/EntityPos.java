package serverutils.lib;

import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;

import latmod.lib.LMUtils;
import latmod.lib.MathHelperLM;

public final class EntityPos implements Cloneable {

    public final double x, y, z;
    public final int dim;

    public EntityPos(double px, double py, double pz, int d) {
        x = px;
        y = py;
        z = pz;
        dim = d;
    }

    public EntityPos(Entity e) {
        this(e.posX, e.posY, e.posZ, e.dimension);
    }

    public int hashCode() {
        return LMUtils.hashCode(x, y, z, dim);
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

    public boolean equalsPos(Entity e) {
        return x == e.posX && y == e.posY && z == e.posZ && dim == e.dimension;
    }

    public boolean equalsPos(EntityPos p) {
        return (p == this) || (p != null && toLinkedPos().equalsPos(p.toLinkedPos()));
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        else if (o == this) return true;
        else if (o instanceof Entity) return equalsPos((Entity) o);
        return equalsPos((EntityPos) o);
    }

    public Vec3 toVec3() {
        return Vec3.createVectorHelper(x, y, z);
    }

    public EntityPos clone() {
        return new EntityPos(x, y, z, dim);
    }

    public BlockDimPos toLinkedPos() {
        return new BlockDimPos(MathHelperLM.floor(x), MathHelperLM.floor(y), MathHelperLM.floor(z), dim);
    }
}
