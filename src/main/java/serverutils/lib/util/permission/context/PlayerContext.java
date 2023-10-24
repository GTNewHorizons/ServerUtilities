package serverutils.lib.util.permission.context;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.google.common.base.Preconditions;

public class PlayerContext extends Context {

    private final EntityPlayer player;

    public PlayerContext(EntityPlayer ep) {
        player = Preconditions.checkNotNull(ep, "Player can't be null in PlayerContext!");
    }

    @Override
    public World getWorld() {
        return player.getEntityWorld();
    }

    @Override
    public EntityPlayer getPlayer() {
        return player;
    }
}
