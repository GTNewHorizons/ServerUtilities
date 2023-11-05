package serverutils.lib.util.permission.context;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class TargetContext extends PlayerContext {

    private final Entity target;

    public TargetContext(EntityPlayer ep, @Nullable Entity entity) {
        super(ep);
        target = entity;
    }

    @Override
    @Nullable
    public <T> T get(ContextKey<T> key) {
        return key.equals(ContextKeys.TARGET) ? (T) target : super.get(key);
    }

    @Override
    protected boolean covers(ContextKey<?> key) {
        return target != null && key.equals(ContextKeys.TARGET);
    }
}
