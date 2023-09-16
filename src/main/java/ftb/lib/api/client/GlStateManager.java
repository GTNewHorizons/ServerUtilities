package ftb.lib.api.client;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * For easier 1.8 port!
 */
public class GlStateManager // GuiLM
{

    public static void color(float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
    }

    public static void translate(float x, float y, float z) {
        GL11.glTranslatef(x, y, z);
    }

    public static void translate(double x, double y, double z) {
        GL11.glTranslated(x, y, z);
    }

    public static void scale(float x, float y, float z) {
        GL11.glScalef(x, y, z);
    }

    public static void scale(double x, double y, double z) {
        GL11.glScaled(x, y, z);
    }

    public static void rotate(float angle, float x, float y, float z) {
        GL11.glRotatef(angle, x, y, z);
    }

    public static void pushMatrix() {
        GL11.glPushMatrix();
    }

    public static void popMatrix() {
        GL11.glPopMatrix();
    }

    public static void pushAttrib() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT);
    }

    public static void popAttrib() {
        GL11.glPopAttrib();
    }

    private static void e(int cap) {
        GL11.glEnable(cap);
    }

    private static void d(int cap) {
        GL11.glDisable(cap);
    }

    public static void enableTexture2D() {
        e(GL11.GL_TEXTURE_2D);
    }

    public static void disableTexture2D() {
        d(GL11.GL_TEXTURE_2D);
    }

    public static void enableBlend() {
        e(GL11.GL_BLEND);
    }

    public static void disableBlend() {
        d(GL11.GL_BLEND);
    }

    public static void blendFunc(int i1, int i2) {
        GL11.glBlendFunc(i1, i2);
    }

    public static void enableDepth() {
        e(GL11.GL_DEPTH_TEST);
    }

    public static void disableDepth() {
        d(GL11.GL_DEPTH_TEST);
    }

    public static void enableLighting() {
        e(GL11.GL_LIGHTING);
    }

    public static void disableLighting() {
        d(GL11.GL_LIGHTING);
    }

    public static void enableRescaleNormal() {
        e(GL12.GL_RESCALE_NORMAL);
    }

    public static void disableRescaleNormal() {
        d(GL12.GL_RESCALE_NORMAL);
    }

    public static void enableColorMaterial() {
        e(GL11.GL_COLOR_MATERIAL);
    }

    public static void disableColorMaterial() {
        d(GL11.GL_COLOR_MATERIAL);
    }

    public static void enableCull() {
        e(GL11.GL_CULL_FACE);
    }

    public static void disableCull() {
        d(GL11.GL_CULL_FACE);
    }

    public static void enableAlpha() {
        e(GL11.GL_ALPHA_TEST);
    }

    public static void disableAlpha() {
        d(GL11.GL_ALPHA_TEST);
    }

    public static void bindTexture(int textureID) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
    }

    public static void depthMask(boolean b) {
        GL11.glDepthMask(b);
    }

    public static void shadeModel(int m) {
        GL11.glShadeModel(m);
    }
}
