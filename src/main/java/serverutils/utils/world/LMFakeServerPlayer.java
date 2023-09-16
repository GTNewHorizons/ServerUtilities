package serverutils.utils.world;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.FakePlayer;

import serverutils.utils.world.ranks.Rank;
import serverutils.utils.world.ranks.Ranks;

public class LMFakeServerPlayer extends LMPlayerServer {

    public final FakePlayer fakePlayer;

    public LMFakeServerPlayer(LMWorldServer w, FakePlayer fp) {
        super(w, Integer.MAX_VALUE, fp.getGameProfile());
        fakePlayer = fp;
    }

    public boolean isOnline() {
        return false;
    }

    public EntityPlayerMP getPlayer() {
        return fakePlayer;
    }

    public boolean isFake() {
        return true;
    }

    public void sendUpdate() {}

    public boolean isOP() {
        return false;
    }

    public void getInfo(LMPlayerServer owner, List<IChatComponent> info) {}

    public void refreshStats() {}

    public void onPostLoaded() {}

    public void checkNewFriends() {}

    public Rank getRank() {
        return Ranks.PLAYER;
    }

    public void claimChunk(int dim, int cx, int cz) {}

    public void unclaimChunk(int dim, int cx, int cz) {}

    public void unclaimAllChunks(Integer dim) {}

    public int getClaimedChunks() {
        return 0;
    }

    public int getLoadedChunks(boolean forced) {
        return 0;
    }

    public void setLoaded(int dim, int cx, int cz, boolean flag) {}
}
