package serverutils.lib.util.permission.context;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;

public class BlockPosContext extends PlayerContext {

    private final int xpos;
    private final int ypos;
    private final int zpos;
    private EnumFacing facing;

    public BlockPosContext(EntityPlayer ep, int x, int y, int z, @Nullable EnumFacing f) {
        super(ep);
        xpos = x;
        ypos = y;
        zpos = z;
        facing = f;
    }

    public BlockPosContext(EntityPlayer ep, ChunkCoordIntPair pos) {
        this(ep, pos.getCenterXPos(), 0, pos.getCenterZPosition(), null);
    }

    @Override
    @Nullable
    public <T> T get(ContextKey<T> key) {
        if (key.equals(ContextKeys.POS)) {
            return (T) Vec3.createVectorHelper(xpos, ypos, zpos);
        }
        // else if(key.equals(ContextKeys.BLOCK_STATE))
        // {
        // if(blockState == null)
        // {
        // blockState = getWorld().getBlockState(blockPos);
        // }

        // return (T) blockState;
        // }
        else if (key.equals(ContextKeys.FACING)) {
            return (T) facing;
        }

        return super.get(key);
    }

    @Override
    protected boolean covers(ContextKey<?> key) {
        return key.equals(ContextKeys.POS)
                || /* key.equals(ContextKeys.BLOCK_STATE) || */ (facing != null && key.equals(ContextKeys.FACING));
    }
}
