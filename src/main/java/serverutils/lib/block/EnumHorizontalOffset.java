package serverutils.lib.block;

import net.minecraft.util.Vec3;

import serverutils.lib.util.IStringSerializable;
import serverutils.lib.util.misc.EnumScreenPosition;
import serverutils.lib.util.misc.NameMap;

public enum EnumHorizontalOffset implements IStringSerializable {

    CENTER("center", EnumScreenPosition.CENTER),
    NORTH("north", EnumScreenPosition.BOTTOM),
    NORTH_EAST("north_east", EnumScreenPosition.BOTTOM_RIGHT),
    EAST("east", EnumScreenPosition.RIGHT),
    SOUTH_EAST("south_east", EnumScreenPosition.TOP_RIGHT),
    SOUTH("south", EnumScreenPosition.TOP),
    SOUTH_WEST("south_west", EnumScreenPosition.TOP_LEFT),
    WEST("west", EnumScreenPosition.LEFT),
    NORTH_WEST("north_west", EnumScreenPosition.BOTTOM_LEFT);

    public static final EnumHorizontalOffset[] VALUES = values();
    private static final EnumHorizontalOffset[] OPPOSITES = { CENTER, SOUTH, SOUTH_WEST, WEST, NORTH_WEST, NORTH,
            NORTH_EAST, EAST, SOUTH_EAST };
    public static final NameMap<EnumHorizontalOffset> NAME_MAP = NameMap.create(CENTER, VALUES);

    private final String name;
    public final EnumScreenPosition screenPosition;
    public final int x_offset;
    public final int y_offset;
    public final int z_offset;

    EnumHorizontalOffset(String n, EnumScreenPosition p) {
        name = n;
        screenPosition = p;
        x_offset = p.offsetX;
        y_offset = 0;
        z_offset = p.offsetY;
    }

    @Override
    public String getName() {
        return name;
    }

    public Vec3 offset(int posx, int posy, int posz) {
        return Vec3.createVectorHelper(x_offset + posx, y_offset + posy, z_offset + posz);
    }

    public boolean isCenter() {
        return this == CENTER;
    }

    public EnumHorizontalOffset opposite() {
        return OPPOSITES[ordinal()];
    }
}
