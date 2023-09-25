package serverutils.utils.net;

import java.util.Collection;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.ChunkCoordIntPair;

import com.feed_the_beast.ftblib.FTBLibNotifications;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.math.ChunkDimPos;
import com.feed_the_beast.ftblib.lib.net.MessageToServer;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.ClaimedChunks;

public class MessageClaimedChunksModify extends MessageToServer {

    public static final int CLAIM = 0;
    public static final int UNCLAIM = 1;
    public static final int LOAD = 2;
    public static final int UNLOAD = 3;

    private int startX, startZ;
    private int action;
    private Collection<ChunkCoordIntPair> chunks;

    public MessageClaimedChunksModify() {}

    public MessageClaimedChunksModify(int sx, int sz, int a, Collection<ChunkCoordIntPair> c) {
        startX = sx;
        startZ = sz;
        action = a;
        chunks = c;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return FTBUtilitiesNetHandler.CLAIMS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(startX);
        data.writeVarInt(startZ);
        data.writeVarInt(action);
        data.writeCollection(chunks, DataOut.CHUNK_POS);
    }

    @Override
    public void readData(DataIn data) {
        startX = data.readVarInt();
        startZ = data.readVarInt();
        action = data.readVarInt();
        chunks = data.readCollection(null, DataIn.CHUNK_POS);
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        if (!ClaimedChunks.isActive()) {
            return;
        }

        ForgePlayer p = ClaimedChunks.instance.universe.getPlayer(player);

        if (!p.hasTeam()) {
            FTBLibNotifications.NO_TEAM.send(player.mcServer, player);
            return;
        }

        boolean jump = true;
        switch (action) {
            case LOAD:
                jump = false;
            case CLAIM:
                for (ChunkCoordIntPair pair : chunks) {
                    ChunkDimPos pos = new ChunkDimPos(pair, player.dimension);
                    if (ClaimedChunks.instance.canPlayerModify(p, pos, ServerUtilitiesPermissions.CLAIMS_OTHER_CLAIM)) {
                        ClaimedChunks.instance.claimChunk(p, pos);
                    }
                }
                if (jump) break;
                ClaimedChunks.instance.processQueue();
                for (ChunkCoordIntPair pair : chunks) {
                    ChunkDimPos pos = new ChunkDimPos(pair, player.dimension);
                    if (ClaimedChunks.instance.canPlayerModify(p, pos, ServerUtilitiesPermissions.CLAIMS_OTHER_LOAD)) {
                        ClaimedChunks.instance.loadChunk(p, p.team, pos);
                    }
                }
                break;
            case UNCLAIM:
                for (ChunkCoordIntPair pair : chunks) {
                    ChunkDimPos pos = new ChunkDimPos(pair, player.dimension);
                    if (ClaimedChunks.instance.canPlayerModify(p, pos, ServerUtilitiesPermissions.CLAIMS_OTHER_UNCLAIM)) {
                        ClaimedChunks.instance.unclaimChunk(p, pos);
                    }
                }
                break;
            case UNLOAD:
                for (ChunkCoordIntPair pair : chunks) {
                    ChunkDimPos pos = new ChunkDimPos(pair, player.dimension);
                    if (ClaimedChunks.instance.canPlayerModify(p, pos, ServerUtilitiesPermissions.CLAIMS_OTHER_UNLOAD)) {
                        ClaimedChunks.instance.unloadChunk(p, pos);
                    }
                }
                break;
        }
    }
}
