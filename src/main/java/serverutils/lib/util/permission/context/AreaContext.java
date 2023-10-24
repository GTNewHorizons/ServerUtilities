package serverutils.lib.util.permission.context;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import com.google.common.base.Preconditions;

public class AreaContext extends PlayerContext {

    private final AxisAlignedBB area;

    public AreaContext(EntityPlayer ep, AxisAlignedBB aabb) {
        super(ep);
        area = Preconditions.checkNotNull(aabb, "AxisAlignedBB can't be null in AreaContext!");
    }

    @Override
    @Nullable
    public <T> T get(ContextKey<T> key) {
        return key.equals(ContextKeys.AREA) ? (T) area : super.get(key);
    }

    @Override
    protected boolean covers(ContextKey<?> key) {
        return key.equals(ContextKeys.AREA);
    }
}
