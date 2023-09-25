package serverutils.utils.net;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import net.minecraft.entity.player.EntityPlayer;

import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.data.ForgeTeam;
import com.feed_the_beast.ftblib.lib.data.Universe;
import com.feed_the_beast.ftblib.lib.gui.misc.ChunkSelectorMap;
import com.feed_the_beast.ftblib.lib.io.Bits;
import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.math.ChunkDimPos;
import com.feed_the_beast.ftblib.lib.net.MessageToClient;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import com.feed_the_beast.ftblib.lib.util.permission.PermissionAPI;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.ClaimedChunk;
import serverutils.utils.data.ClaimedChunks;
import serverutils.utils.data.FTBUtilitiesTeamData;
import serverutils.utils.events.chunks.UpdateClientDataEvent;
import serverutils.utils.gui.ClientClaimedChunks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author LatvianModder
 */
public class MessageClaimedChunksUpdate extends MessageToClient {

    public int startX, startZ, claimedChunks, loadedChunks, maxClaimedChunks, maxLoadedChunks;
    public Map<Short, ClientClaimedChunks.Team> teams;

    public MessageClaimedChunksUpdate() {}

    public MessageClaimedChunksUpdate(int sx, int sz, EntityPlayer player) {
        startX = sx;
        startZ = sz;

        ForgePlayer p = Universe.get().getPlayer(player);
        FTBUtilitiesTeamData teamData = FTBUtilitiesTeamData.get(p.team);

        Collection<ClaimedChunk> chunks = teamData.team.isValid()
                ? ClaimedChunks.instance.getTeamChunks(teamData.team, OptionalInt.empty())
                : Collections.emptyList();

        claimedChunks = chunks.size();
        loadedChunks = 0;

        for (ClaimedChunk c : chunks) {
            if (c.isLoaded()) {
                loadedChunks++;
            }
        }

        maxClaimedChunks = teamData.getMaxClaimChunks();
        maxLoadedChunks = teamData.getMaxChunkloaderChunks();
        teams = new HashMap<>();

        boolean canSeeChunkInfo = PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CLAIMS_OTHER_SEE_INFO);

        for (int x1 = 0; x1 < ChunkSelectorMap.TILES_GUI; x1++) {
            for (int z1 = 0; z1 < ChunkSelectorMap.TILES_GUI; z1++) {
                ChunkDimPos pos = new ChunkDimPos(startX + x1, startZ + z1, player.dimension);
                ClaimedChunk chunk = ClaimedChunks.instance.getChunk(pos);

                if (chunk != null) {
                    ForgeTeam chunkTeam = chunk.getTeam();

                    if (!chunkTeam.isValid()) {
                        continue;
                    }

                    ClientClaimedChunks.Team team = teams.get(chunkTeam.getUID());

                    if (team == null) {
                        team = new ClientClaimedChunks.Team(chunkTeam.getUID());
                        team.color = chunkTeam.getColor();
                        team.nameComponent = chunkTeam.getTitle();
                        team.isAlly = chunkTeam.isAlly(p);
                        teams.put(chunkTeam.getUID(), team);
                    }

                    boolean member = chunkTeam.isMember(p);
                    int flags = 0;

                    if (canSeeChunkInfo || member) {
                        if (chunk.isLoaded()) {
                            flags = Bits.setFlag(flags, ClientClaimedChunks.ChunkData.LOADED, true);
                        }
                    }

                    team.chunks
                            .put(x1 + z1 * ChunkSelectorMap.TILES_GUI, new ClientClaimedChunks.ChunkData(team, flags));
                }
            }
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return FTBUtilitiesNetHandler.CLAIMS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(startX);
        data.writeVarInt(startZ);
        data.writeVarInt(claimedChunks);
        data.writeVarInt(loadedChunks);
        data.writeVarInt(maxClaimedChunks);
        data.writeVarInt(maxLoadedChunks);
        data.writeCollection(teams.values(), ClientClaimedChunks.Team.SERIALIZER);
    }

    @Override
    public void readData(DataIn data) {
        startX = data.readVarInt();
        startZ = data.readVarInt();
        claimedChunks = data.readVarInt();
        loadedChunks = data.readVarInt();
        maxClaimedChunks = data.readVarInt();
        maxLoadedChunks = data.readVarInt();

        teams = new HashMap<>();

        for (ClientClaimedChunks.Team team : data.readCollection(ClientClaimedChunks.Team.DESERIALIZER)) {
            teams.put(team.uid, team);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new UpdateClientDataEvent(this).post();
    }
}
