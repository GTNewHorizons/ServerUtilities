package serverutils.lib.util.permission.context;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.google.common.base.Preconditions;

public class WorldContext extends Context {

    private final World world;

    public WorldContext(World w) {
        world = Preconditions.checkNotNull(w, "World can't be null in WorldContext!");
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    @Nullable
    public EntityPlayer getPlayer() {
        return null;
    }
}
