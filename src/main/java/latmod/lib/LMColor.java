package latmod.lib;

/**
 * Created by LatvianModder on 08.01.2016.
 */
public abstract class LMColor {

    public static final LMColor WHITE = new ImmutableColor(0xFFFFFFFF);
    public static final LMColor BLACK = new ImmutableColor(0xFF000000);
    public static final LMColor TRANSPARENT = new ImmutableColor(0x00000000);

    public static class RGB extends LMColor {

        private int red = 255, green = 255, blue = 255;

        public RGB() {}

        public RGB(int r, int g, int b) {
            setRGBA(r, g, b, 255);
        }

        public void set(LMColor col) {
            red = col.red();
            green = col.green();
            blue = col.blue();
        }

        public void setRGBA(int r, int g, int b, int a) {
            red = r;
            green = g;
            blue = b;
        }

        public void setHSB(float h, float s, float b) {
            setRGBA(0xFF000000 | java.awt.Color.HSBtoRGB(h, s, b));
        }

        public void setRGBA(int col) {
            red = LMColorUtils.getRed(col);
            green = LMColorUtils.getGreen(col);
            blue = LMColorUtils.getBlue(col);
        }

        public void setRGBAF(float r, float g, float b, float a) {
            setRGBA((int) (r * 255F), (int) (g * 255F), (int) (b * 255F), (int) (a * 255F));
        }

        public int color() {
            return LMColorUtils.getRGBA(red(), green(), blue(), alpha());
        }

        public int red() {
            return red;
        }

        public int green() {
            return green;
        }

        public int blue() {
            return blue;
        }

        public int alpha() {
            return 255;
        }

        public float hue() {
            float[] hsb = new float[3];
            java.awt.Color.RGBtoHSB(red, green, blue, hsb);
            return hsb[0];
        }

        public float saturation() {
            float[] hsb = new float[3];
            java.awt.Color.RGBtoHSB(red, green, blue, hsb);
            return hsb[1];
        }

        public float brightness() {
            float[] hsb = new float[3];
            java.awt.Color.RGBtoHSB(red, green, blue, hsb);
            return hsb[2];
        }

        public RGB copy() {
            RGB col = new RGB();
            col.set(this);
            return col;
        }
    }

    public static class RGBA extends RGB {

        private int alpha = 255;

        public RGBA() {}

        public RGBA(int r, int g, int b, int a) {
            setRGBA(r, g, b, a);
        }

        public void setRGBA(int col) {
            super.setRGBA(col);
            alpha = LMColorUtils.getAlpha(col);
        }

        public void setRGBA(int r, int g, int b, int a) {
            super.setRGBA(r, g, b, a);
            alpha = a;
        }

        public int alpha() {
            return alpha;
        }
    }

    public static class HSB extends LMColor {

        private int color;
        private final float[] hsb;

        public HSB(float h, float s, float b) {
            hsb = new float[3];
            setHSB(h, s, b);
        }

        public HSB() {
            this(0F, 1F, 1F);
        }

        public void set(LMColor col) {
            color = 0xFF000000 | col.color();
            hsb[0] = col.hue();
            hsb[1] = col.saturation();
            hsb[2] = col.brightness();
        }

        public void setRGBA(int rgba) {
            setRGBA(LMColorUtils.getRed(rgba), LMColorUtils.getGreen(rgba), LMColorUtils.getBlue(rgba), 255);
        }

        public void setRGBA(int r, int g, int b, int a) {
            color = LMColorUtils.getRGBA(r, g, b, 255);
            java.awt.Color.RGBtoHSB(r, g, b, hsb);
        }

        public void setHSB(float h, float s, float b) {
            hsb[0] = h % 1F;
            hsb[1] = MathHelperLM.clampFloat(s, 0F, 1F);
            hsb[2] = MathHelperLM.clampFloat(b, 0F, 1F);
            color = 0xFF000000 | java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        }

        public void addHue(float hue) {
            setHSB(hsb[0] + hue, hsb[1], hsb[2]);
        }

        public int color() {
            return color;
        }

        public int red() {
            return LMColorUtils.getRed(color);
        }

        public int green() {
            return LMColorUtils.getGreen(color);
        }

        public int blue() {
            return LMColorUtils.getBlue(color);
        }

        public int alpha() {
            return 255;
        }

        public float hue() {
            return hsb[0];
        }

        public float saturation() {
            return hsb[1];
        }

        public float brightness() {
            return hsb[2];
        }

        public HSB copy() {
            HSB col = new HSB();
            col.set(this);
            return col;
        }
    }

    public abstract void set(LMColor col);

    public abstract void setRGBA(int rgba);

    public abstract void setRGBA(int r, int g, int b, int a);

    public abstract void setHSB(float h, float s, float b);

    public abstract int color();

    public abstract int red();

    public abstract int green();

    public abstract int blue();

    public abstract int alpha();

    public abstract float hue();

    public abstract float saturation();

    public abstract float brightness();

    public abstract LMColor copy();

    public final String toString() {
        return LMColorUtils.getHex(color());
    }

    public final int hashCode() {
        return color();
    }

    public final boolean equals(Object o) {
        return hashCode() == o.hashCode();
    }

    public final LMColor immutable() {
        return new ImmutableColor(this);
    }

    public static class ImmutableColor extends RGBA {

        public ImmutableColor(LMColor col) {
            super.set(col);
        }

        public ImmutableColor(int col) {
            super.setRGBA(col);
        }

        public void set(LMColor col) {}

        public void setRGBA(int rgba) {}

        public void setRGBA(int r, int g, int b, int a) {}

        public void setHSB(float h, float s, float b) {}

        public ImmutableColor copy() {
            return new ImmutableColor(this);
        }
    }
}
