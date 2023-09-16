package ftb.lib;

import net.minecraft.entity.player.EntityPlayer;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.relauncher.Side;
import ftb.lib.api.friends.ILMPlayer;

/**
 * Created by LatvianModder on 15.01.2016.
 */
public class TempLMPlayerFromEntity implements ILMPlayer {

    public final Side side;
    public final EntityPlayer entity;

    public TempLMPlayerFromEntity(Side s, EntityPlayer ep) {
        side = s;
        entity = ep;
    }

    public Side getSide() {
        return side;
    }

    public int getPlayerID() {
        return 0;
    }

    public boolean isOnline() {
        return true;
    }

    public EntityPlayer getPlayer() {
        return entity;
    }

    public GameProfile getProfile() {
        return entity.getGameProfile();
    }

    public boolean isFriendRaw(ILMPlayer p) {
        return true;
    }

    public boolean isFriend(ILMPlayer p) {
        return true;
    }

    public boolean allowInteractSecure() {
        return false;
    }
}
