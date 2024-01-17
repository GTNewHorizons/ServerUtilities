package serverutils.lib.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;

import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.MutableColor4I;
import serverutils.lib.math.MathUtils;

public class CachedVertexData {

    private final int mode;
    private final List<CachedVertex> list;
    private final boolean hasTex, hasColor, hasNormal;
    public final MutableColor4I color;
    public double minU = 0D, minV = 0D, maxU = 1D, maxV = 1D;

    private CachedVertexData(int m, boolean tex, boolean color, boolean normal, Collection<CachedVertex> oldList) {
        mode = m;
        list = new ArrayList<>(oldList);
        hasTex = tex;
        hasColor = color;
        hasNormal = normal;
        this.color = Color4I.WHITE.mutable();
    }

    public CachedVertexData(int m, boolean tex, boolean color, boolean normal) {
        this(m, tex, color, normal, Collections.emptyList());
    }

    public void reset() {
        list.clear();
    }

    public CachedVertexData copy() {
        return new CachedVertexData(mode, hasTex, hasColor, hasNormal, list);
    }

    public CachedVertex pos(double x, double y, double z) {
        CachedVertex v = new CachedVertex(x, y, z);
        list.add(v);
        return v;
    }

    public CachedVertex pos(double x, double y) {
        return pos(x, y, 0D);
    }

    public void draw(Tessellator tessellator) {
        if (list.isEmpty()) {
            return;
        }

        tessellator.startDrawing(mode);
        for (CachedVertex v : list) {

            v.appendTo(tessellator);
        }
        tessellator.draw();
    }

    public class CachedVertex {

        private final double x, y, z;
        private double u, v;
        private int r = color.redi(), g = color.greeni(), b = color.bluei(), a = color.alphai();
        private float nx, ny, nz;

        private CachedVertex(double _x, double _y, double _z) {
            x = _x;
            y = _y;
            z = _z;
        }

        public CachedVertex tex(double _u, double _v) {
            u = _u;
            v = _v;
            return this;
        }

        public CachedVertex color(int _r, int _g, int _b, int _a) {
            r = _r;
            g = _g;
            b = _b;
            a = _a;
            return this;
        }

        public CachedVertex normal(float x, float y, float z) {
            nx = x;
            ny = y;
            nz = z;
            return this;
        }

        private void appendTo(Tessellator tessellator) {

            if (hasTex) {
                tessellator.setTextureUV(u, v);
            }

            if (hasColor) {
                tessellator.setColorRGBA(r, g, b, a);
            }

            if (hasNormal) {
                tessellator.setNormal(nx, ny, nz);
            }

            tessellator.addVertex(x, y, z);
        }
    }

    public void rect(int x, int y, int w, int h) {
        pos(x, y + h, 0D).tex(minU, maxV);
        pos(x + w, y + h, 0D).tex(maxU, maxV);
        pos(x + w, y, 0D).tex(maxU, minV);
        pos(x, y, 0D).tex(minU, minV);
    }

    public void rect(int x, int y, int w, int h, int z) {
        pos(x, y + h, z).tex(minU, maxV);
        pos(x + w, y + h, z).tex(maxU, maxV);
        pos(x + w, y, z).tex(maxU, minV);
        pos(x, y, z).tex(minU, minV);
    }

    public void cubeFace(EnumFacing facing, double minX, double minY, double minZ, double maxX, double maxY,
            double maxZ) {
        float normalX = MathUtils.NORMALS_X[facing.order_a];
        float normalY = MathUtils.NORMALS_Y[facing.order_a];
        float normalZ = MathUtils.NORMALS_Z[facing.order_a];

        switch (facing) {
            case DOWN:
                pos(minX, minY, minZ).tex(minU, minV).normal(normalX, normalY, normalZ);
                pos(maxX, minY, minZ).tex(maxU, minV).normal(normalX, normalY, normalZ);
                pos(maxX, minY, maxZ).tex(maxU, maxV).normal(normalX, normalY, normalZ);
                pos(minX, minY, maxZ).tex(minU, maxV).normal(normalX, normalY, normalZ);
                break;
            case UP:
                pos(minX, maxY, minZ).tex(minU, minV).normal(normalX, normalY, normalZ);
                pos(minX, maxY, maxZ).tex(minU, maxV).normal(normalX, normalY, normalZ);
                pos(maxX, maxY, maxZ).tex(maxU, maxV).normal(normalX, normalY, normalZ);
                pos(maxX, maxY, minZ).tex(maxU, minV).normal(normalX, normalY, normalZ);
                break;
            case NORTH:
                pos(minX, minY, minZ).tex(maxU, maxV).normal(normalX, normalY, normalZ);
                pos(minX, maxY, minZ).tex(maxU, minV).normal(normalX, normalY, normalZ);
                pos(maxX, maxY, minZ).tex(minU, minV).normal(normalX, normalY, normalZ);
                pos(maxX, minY, minZ).tex(minU, maxV).normal(normalX, normalY, normalZ);
                break;
            case SOUTH:
                pos(minX, minY, maxZ).tex(minU, maxV).normal(normalX, normalY, normalZ);
                pos(maxX, minY, maxZ).tex(maxU, maxV).normal(normalX, normalY, normalZ);
                pos(maxX, maxY, maxZ).tex(maxU, minV).normal(normalX, normalY, normalZ);
                pos(minX, maxY, maxZ).tex(minU, minV).normal(normalX, normalY, normalZ);
                break;
            case WEST:
                pos(minX, minY, minZ).tex(minU, maxV).normal(normalX, normalY, normalZ);
                pos(minX, minY, maxZ).tex(maxU, maxV).normal(normalX, normalY, normalZ);
                pos(minX, maxY, maxZ).tex(maxU, minV).normal(normalX, normalY, normalZ);
                pos(minX, maxY, minZ).tex(minU, minV).normal(normalX, normalY, normalZ);
                break;
            case EAST:
                pos(maxX, minY, minZ).tex(maxU, maxV).normal(normalX, normalY, normalZ);
                pos(maxX, maxY, minZ).tex(maxU, minV).normal(normalX, normalY, normalZ);
                pos(maxX, maxY, maxZ).tex(minU, minV).normal(normalX, normalY, normalZ);
                pos(maxX, minY, maxZ).tex(minU, maxV).normal(normalX, normalY, normalZ);
                break;
        }
    }

    public void cube(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        for (EnumFacing facing : EnumFacing.faceList) {
            cubeFace(facing, minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public void cubeSides(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        for (EnumFacing facing : EnumFacing.faceList) {
            if (facing.order_a != 0 && facing.order_a != 1) {
                cubeFace(facing, minX, minY, minZ, maxX, maxY, maxZ);
            }
        }
    }

    public void centeredCube(double x, double y, double z, double rx, double ry, double rz) {
        cube(x - rx, y - ry, z - rz, x + rx, y + ry, z + rz);

        /*
         * pos(x + rx, y + ry, z - rz); pos(x - rx, y + ry, z - rz); pos(x - rx, y + ry, z + rz); pos(x + rx, y + ry, z
         * + rz); pos(x + rx, y - ry, z + rz); pos(x - rx, y - ry, z + rz); pos(x - rx, y - ry, z - rz); pos(x + rx, y -
         * ry, z - rz); pos(x + rx, y + ry, z + rz); pos(x - rx, y + ry, z + rz); pos(x - rx, y - ry, z + rz); pos(x +
         * rx, y - ry, z + rz); pos(x + rx, y - ry, z - rz); pos(x - rx, y - ry, z - rz); pos(x - rx, y + ry, z - rz);
         * pos(x + rx, y + ry, z - rz); pos(x - rx, y + ry, z + rz); pos(x - rx, y + ry, z - rz); pos(x - rx, y - ry, z
         * - rz); pos(x - rx, y - ry, z + rz); pos(x + rx, y + ry, z - rz); pos(x + rx, y + ry, z + rz); pos(x + rx, y -
         * ry, z + rz) ; pos(x + rx, y - ry, z - rz);
         */
    }

    public void centeredCube(double x, double y, double z, double r) {
        centeredCube(x, y, z, r, r, r);
    }

    public void cube(AxisAlignedBB aabb) {
        cube(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }
}
