package serverutils.lib;

import net.minecraft.util.ResourceLocation;

import latmod.lib.LMUtils;

public final class TextureCoords {

    public static final TextureCoords nullTexture = new TextureCoords(null, 0D, 0D, 0D, 0D, 0D, 0D);

    public final ResourceLocation texture;
    public final double posX, posY, width, height;
    public final double textureW, textureH;
    public final double minU, minV, maxU, maxV;

    private String toString = null;

    public TextureCoords(ResourceLocation res, double x, double y, double w, double h, double tw, double th) {
        texture = res;
        posX = x;
        posY = y;
        width = w;
        height = h;
        textureW = tw;
        textureH = th;

        minU = posX / textureW;
        minV = posY / textureH;
        maxU = (posX + width) / textureW;
        maxV = (posY + height) / textureH;
    }

    public TextureCoords(ResourceLocation res, int x, int y, int w, int h) {
        this(res, x, y, w, h, 256D, 256D);
    }

    public static TextureCoords getSquareIcon(ResourceLocation res, int size) {
        return new TextureCoords(res, 0D, 0D, size, size, size, size);
    }

    public int posXI() {
        return (int) posX;
    }

    public int posYI() {
        return (int) posY;
    }

    public int widthI() {
        return (int) width;
    }

    public int heightI() {
        return (int) height;
    }

    public int hashCode() {
        return LMUtils.hashCode(texture, posX, posY, width, height);
    }

    public String toString() {
        if (toString == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(texture);
            sb.append(',');
            sb.append(posXI());
            sb.append(',');
            sb.append(posYI());
            sb.append(',');
            sb.append(widthI());
            sb.append(',');
            sb.append(heightI());
            toString = sb.toString();
        }

        return toString;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        else if (o == this) return true;
        else {
            TextureCoords t = (TextureCoords) o;
            return LMUtils.areObjectsEqual(texture, t.texture, true) && minU == t.minU
                    && minV == t.minV
                    && maxU == t.maxU
                    && maxV == t.maxV;
        }
    }

    public double getWidth(double h) {
        return width * (h / height);
    }

    public double getHeight(double w) {
        return height * (w / width);
    }

    public boolean isValid() {
        return texture != null && width > 0 && height > 0;
    }

    public TextureCoords copy() {
        return new TextureCoords(texture, posX, posY, width, height, textureW, textureH);
    }

    public TextureCoords[] split(int x, int y) {
        if (x == 0 || y == 0) return new TextureCoords[0];
        if (x == 1 && y == 1) return new TextureCoords[] { copy() };
        if (x == 1) return splitY(y);
        if (y == 1) return splitX(x);

        TextureCoords[] l = new TextureCoords[x * y];
        TextureCoords[] ly = splitY(y);

        for (int y1 = 0; y1 < y; y1++) {
            l[y1 * x] = ly[y1];
            TextureCoords[] lx = ly[y1].splitX(x);
            for (int x1 = 0; x1 < x; x1++) l[y1 * x + x1] = lx[x1];
        }

        return l;
    }

    private TextureCoords[] splitX(int s) {
        TextureCoords[] l = new TextureCoords[s];
        double ds = (double) s;
        double d = width / ds;
        for (int i = 0; i < s; i++)
            l[i] = new TextureCoords(texture, posX + d * i, posY, d, height, textureW, textureH);
        return l;
    }

    private TextureCoords[] splitY(int s) {
        TextureCoords[] l = new TextureCoords[s];
        double ds = (double) s;
        double d = height / ds;
        for (int i = 0; i < s; i++) l[i] = new TextureCoords(texture, posX, posY + d * i, width, d, textureW, textureH);
        return l;
    }
}
