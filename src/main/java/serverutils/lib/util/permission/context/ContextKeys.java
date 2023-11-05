package serverutils.lib.util.permission.context;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

/**
 * Some default context keys, for easier compatibility
 */
public class ContextKeys {

    /**
     * BlockPos for interacting, breaking and other permissions
     */
    public static final ContextKey<Vec3> POS = ContextKey.create("pos", Vec3.class);

    /**
     * The entity can be anything that gets interacted with - a sheep when you try to dye it, skeleton that you attack,
     * etc.
     */
    public static final ContextKey<Entity> TARGET = ContextKey.create("target", Entity.class);

    public static final ContextKey<EnumFacing> FACING = ContextKey.create("facing", EnumFacing.class);
    public static final ContextKey<AxisAlignedBB> AREA = ContextKey.create("area", AxisAlignedBB.class);
    // public static final ContextKey<IBlockState> BLOCK_STATE = ContextKey.create("blockstate", IBlockState.class);
}
