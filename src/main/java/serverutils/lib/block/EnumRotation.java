package serverutils.lib.block;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

import serverutils.lib.util.IStringSerializable;

public enum EnumRotation implements IStringSerializable {

    NORMAL("normal"),
    FACING_DOWN("down"),
    UPSIDE_DOWN("upside_down"),
    FACING_UP("up");

    public static final EnumRotation[] VALUES = values();

    private final String name;

    EnumRotation(String n) {
        name = n;
    }

    public int getModelRotationIndexFromFacing(EnumFacing facing) {
        return ordinal() << 2 | facing.order_b;
    }

    @Override
    public String getName() {
        return name;
    }

    public static EnumRotation getRotationFromEntity(int posx, int posy, int posz, EntityLivingBase placer) {
        if (MathHelper.abs((float) (placer.posX - posx)) < 2F && MathHelper.abs((float) (placer.posZ - posz)) < 2F) {
            double d = placer.posY + placer.getEyeHeight();

            if (d - posy > 2D) {
                return FACING_UP;
            } else if (posy - d > 0D) {
                return FACING_DOWN;
            }
        }

        return NORMAL;
    }
}
