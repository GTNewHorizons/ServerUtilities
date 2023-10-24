package serverutils.lib.util.misc;

import serverutils.lib.util.IStringSerializable;

public enum EnumScreenPosition implements IStringSerializable {

    CENTER("center", 0, 0),
    TOP("top", 0, -1),
    BOTTOM("bottom", 0, 1),
    LEFT("left", -1, 0),
    RIGHT("right", 1, 0),
    TOP_LEFT("top_left", -1, -1),
    TOP_RIGHT("top_right", 1, -1),
    BOTTOM_LEFT("bottom_left", -1, 1),
    BOTTOM_RIGHT("bottom_right", 1, 1);

    public static final NameMap<EnumScreenPosition> NAME_MAP = NameMap.create(CENTER, values());

    private final String name;
    public final int offsetX, offsetY;

    EnumScreenPosition(String n, int ox, int oy) {
        name = n;
        offsetX = ox;
        offsetY = oy;
    }

    @Override
    public String getName() {
        return name;
    }

    public int getX(int screenWidth, int width, int offset) {
        return switch (offsetX) {
            case -1 -> offset;
            case 1 -> (screenWidth - width) / 2;
            default -> screenWidth - width - offset;
        };
    }

    public int getY(int screenHeight, int height, int offset) {
        return switch (offsetY) {
            case -1 -> offset;
            case 1 -> (screenHeight - height) / 2;
            default -> screenHeight - height - offset;
        };
    }
}
