package serverutils.lib.client;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class GlStateManager {

    public static int glGetError() {
        return GL11.glGetError();
    }

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

    public static void blendFunc(SourceFactor fSourceFactor, DestFactor fDestFactor) {
        GL11.glBlendFunc(fSourceFactor.factor, fDestFactor.factor);
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

    public static enum DestFactor {

        CONSTANT_ALPHA(32771),
        CONSTANT_COLOR(32769),
        DST_ALPHA(772),
        DST_COLOR(774),
        ONE(1),
        ONE_MINUS_CONSTANT_ALPHA(32772),
        ONE_MINUS_CONSTANT_COLOR(32770),
        ONE_MINUS_DST_ALPHA(773),
        ONE_MINUS_DST_COLOR(775),
        ONE_MINUS_SRC_ALPHA(771),
        ONE_MINUS_SRC_COLOR(769),
        SRC_ALPHA(770),
        SRC_COLOR(768),
        ZERO(0);

        public final int factor;

        private DestFactor(int factorIn) {
            this.factor = factorIn;
        }
    }

    public static enum SourceFactor {

        CONSTANT_ALPHA(32771),
        CONSTANT_COLOR(32769),
        DST_ALPHA(772),
        DST_COLOR(774),
        ONE(1),
        ONE_MINUS_CONSTANT_ALPHA(32772),
        ONE_MINUS_CONSTANT_COLOR(32770),
        ONE_MINUS_DST_ALPHA(773),
        ONE_MINUS_DST_COLOR(775),
        ONE_MINUS_SRC_ALPHA(771),
        ONE_MINUS_SRC_COLOR(769),
        SRC_ALPHA(770),
        SRC_ALPHA_SATURATE(776),
        SRC_COLOR(768),
        ZERO(0);

        public final int factor;

        private SourceFactor(int factorIn) {
            this.factor = factorIn;
        }
    }

    public static void glLineWidth(float f) {
        GL11.glLineWidth(f);
    }

    public static enum LogicOp {

        AND(5377),
        AND_INVERTED(5380),
        AND_REVERSE(5378),
        CLEAR(5376),
        COPY(5379),
        COPY_INVERTED(5388),
        EQUIV(5385),
        INVERT(5386),
        NAND(5390),
        NOOP(5381),
        NOR(5384),
        OR(5383),
        OR_INVERTED(5389),
        OR_REVERSE(5387),
        SET(5391),
        XOR(5382);

        public final int opcode;

        private LogicOp(int opcodeIn) {
            this.opcode = opcodeIn;
        }
    }

    public static void colorLogicOp(int opcode) {
        GL11.glLogicOp(opcode);
    }

    public static void enableColorLogic() {
        GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
    }

    public static void disableColorLogic() {
        GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
    }

    public static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha) {
        OpenGlHelper.glBlendFunc(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
    }

    public static void blendFunc(int src, int dst) {
        GL11.glBlendFunc(src, dst);
    }

    public static void clearColor(float r, float g, float b, float a) {
        GL11.glClearColor(r, g, b, a);
    }

    public static void clear(int mask) {
        GL11.glClear(mask);
    }
}
