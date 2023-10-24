package serverutils.lib.config;

import serverutils.lib.icon.Color4I;

public class ConfigRGB {

    public int red;
    public int green;
    public int blue;

    public ConfigRGB(int r, int g, int b) {
        red = r & 0xFF;
        green = g & 0xFF;
        blue = b & 0xFF;
    }

    public ConfigRGB(Color4I col) {
        red = col.redi();
        green = col.greeni();
        blue = col.bluei();
    }

    public Color4I createColor(int a) {
        return Color4I.rgba(red, green, blue, a);
    }

    public Color4I createColor() {
        return createColor(255);
    }
}
