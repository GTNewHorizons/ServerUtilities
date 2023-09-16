package serverutils.lib.api.client.model;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CubeRenderer {

    public static final CubeRenderer instance = new CubeRenderer();
    private Tessellator tessellator;

    protected static final float[] normalsX = new float[] { 0F, 0F, 0F, 0F, -1F, 1F };
    protected static final float[] normalsY = new float[] { -1F, 1F, 0F, 0F, 0F, 0F };
    protected static final float[] normalsZ = new float[] { 0F, 0F, -1F, 1F, 0F, 0F };

    public boolean hasTexture = true;

    /**
     * Unimplemented
     */
    public boolean isInterpolated = false;

    protected int currentFace = -1;
    protected double minX, minY, minZ, maxX, maxY, maxZ;
    protected double minU, minV, maxU, maxV;

    public CubeRenderer() {
        setTessellator(Tessellator.instance);
    }

    public void setTessellator(Tessellator t) {
        if (t == null) return;
        tessellator = t;
    }

    public void setSize(double x0, double y0, double z0, double x1, double y1, double z1) {
        minX = x0;
        minY = y0;
        minZ = z0;
        maxX = x1;
        maxY = y1;
        maxZ = z1;
    }

    public void setSize(AxisAlignedBB aabb) {
        setSize(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    public void setUV(double u0, double v0, double u1, double v1) {
        minU = u0;
        minV = v0;
        maxU = u1;
        maxV = v1;
    }

    public void renderAll() {
        renderDown();
        renderUp();
        renderSouth();
        renderNorth();
        renderWest();
        renderEast();
    }

    public void renderFace(int f) {
        if (f == 0) renderDown();
        else if (f == 1) renderUp();
        else if (f == 2) renderSouth();
        else if (f == 3) renderNorth();
        else if (f == 4) renderWest();
        else if (f == 5) renderEast();
    }

    private void begin(int f) {
        currentFace = f;
        tessellator.startDrawingQuads();
        tessellator.setNormal(normalsX[f], normalsY[f], normalsZ[f]);
    }

    private void end() {
        tessellator.draw();
        currentFace = -1;
    }

    private void vertex(double x, double y, double z, double u, double v) {
        if (hasTexture) tessellator.addVertexWithUV(x, y, z, u, v);
        else tessellator.addVertex(x, y, z);
    }

    public void renderDown() {
        begin(0);
        vertex(minX, minY, minZ, minU, minV);
        vertex(maxX, minY, minZ, maxU, minV);
        vertex(maxX, minY, maxZ, maxU, maxV);
        vertex(minX, minY, maxZ, minU, maxV);
        end();
    }

    public void renderUp() {
        begin(1);
        vertex(minX, maxY, minZ, minU, minV);
        vertex(minX, maxY, maxZ, minU, maxV);
        vertex(maxX, maxY, maxZ, maxU, maxV);
        vertex(maxX, maxY, minZ, maxU, minV);
        end();
    }

    public void renderSouth() {
        begin(2);
        vertex(minX, minY, maxZ, minU, maxV);
        vertex(maxX, minY, maxZ, maxU, maxV);
        vertex(maxX, maxY, maxZ, maxU, minV);
        vertex(minX, maxY, maxZ, minU, minV);
        end();
    }

    public void renderNorth() {
        begin(3);
        vertex(minX, minY, minZ, maxU, maxV);
        vertex(minX, maxY, minZ, maxU, minV);
        vertex(maxX, maxY, minZ, minU, minV);
        vertex(maxX, minY, minZ, minU, maxV);
        end();
    }

    public void renderWest() {
        begin(4);
        vertex(minX, minY, minZ, minU, maxV);
        vertex(minX, minY, maxZ, maxU, maxV);
        vertex(minX, maxY, maxZ, maxU, minV);
        vertex(minX, maxY, minZ, minU, minV);
        end();
    }

    public void renderEast() {
        begin(5);
        vertex(maxX, minY, minZ, maxU, maxV);
        vertex(maxX, maxY, minZ, maxU, minV);
        vertex(maxX, maxY, maxZ, minU, minV);
        vertex(maxX, minY, maxZ, minU, maxV);
        end();
    }
}
