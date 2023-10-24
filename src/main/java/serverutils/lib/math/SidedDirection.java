package serverutils.lib.math;

import net.minecraft.util.EnumFacing;

public enum SidedDirection {

    BOTTOM(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.DOWN, EnumFacing.DOWN, EnumFacing.DOWN, EnumFacing.DOWN),
    TOP(EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.UP, EnumFacing.UP, EnumFacing.UP, EnumFacing.UP),
    BACK(EnumFacing.UP, EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.WEST),
    FRONT(EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST),
    LEFT(EnumFacing.WEST, EnumFacing.EAST, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH),
    RIGHT(EnumFacing.EAST, EnumFacing.WEST, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.NORTH);

    public static final SidedDirection[] VALUES = values();
    public final EnumFacing[] directions;

    private static final SidedDirection[] CACHED_SIDES = new SidedDirection[36];

    static {
        for (EnumFacing side : EnumFacing.faceList) {
            for (EnumFacing rot : EnumFacing.faceList) {
                for (SidedDirection d : VALUES) {
                    if (d.directions[rot.ordinal()] == side) {
                        CACHED_SIDES[side.ordinal() + rot.ordinal() * 6] = d;
                        break;
                    }
                }
            }
        }
    }

    // Static //

    SidedDirection(EnumFacing a, EnumFacing b, EnumFacing c, EnumFacing d, EnumFacing e, EnumFacing f) {
        directions = new EnumFacing[] { a, b, c, d, e, f };
    }

    public static SidedDirection getSide(EnumFacing side, EnumFacing rot) {
        return CACHED_SIDES[side.ordinal() + rot.ordinal() * 6];
    }

    public static SidedDirection get(EnumFacing side, EnumFacing rot3D, EnumFacing rot2D) {
        if (side == rot3D) {
            return FRONT;
        } else if (side == EnumFacing.faceList[rot3D.order_b]) {
            return BACK;
        } else if (rot3D == EnumFacing.DOWN) {
            if (rot2D == side) {
                return TOP;
            } else if (rot2D == EnumFacing.faceList[side.order_b]) {
                return BOTTOM;
            }
            return getSide(side, rot2D);
        } else if (rot3D == EnumFacing.UP) {
            if (rot2D == side) {
                return BOTTOM;
            } else if (rot2D == EnumFacing.faceList[side.order_b]) {
                return TOP;
            }
            return getSide(side, rot2D);
        } else {
            if (side == EnumFacing.DOWN) {
                return BOTTOM;
            } else if (side == EnumFacing.UP) {
                return TOP;
            }
            return getSide(side, rot3D);
        }
    }
}
