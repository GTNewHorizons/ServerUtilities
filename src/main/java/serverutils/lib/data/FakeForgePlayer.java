package serverutils.lib.data;

import net.minecraft.entity.player.EntityPlayerMP;

import com.mojang.authlib.GameProfile;

import serverutils.lib.util.ServerUtils;

public class FakeForgePlayer extends ForgePlayer {

    public FakeForgePlayer(Universe u) {
        super(u, ServerUtils.FAKE_PLAYER_PROFILE.getId(), ServerUtils.FAKE_PLAYER_PROFILE.getName());
    }

    @Override
    public GameProfile getProfile() {
        return ServerUtils.FAKE_PLAYER_PROFILE;
    }

    @Override
    public boolean isOnline() {
        return tempPlayer != null;
    }

    @Override
    public EntityPlayerMP getPlayer() {
        if (tempPlayer == null) {
            throw new NullPointerException("Fake player not set yet!");
        }

        return tempPlayer;
    }

    @Override
    public boolean isFake() {
        return true;
    }

    @Override
    public boolean isOP() {
        return false;
    }

    @Override
    public void markDirty() {
        team.universe.markDirty();
    }
}
