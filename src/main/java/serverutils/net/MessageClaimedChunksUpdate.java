package serverutils.net;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.ServerUtilitiesPermissions;
import serverutils.client.gui.ClientClaimedChunks;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesTeamData;
import serverutils.events.chunks.UpdateClientDataEvent;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;
import serverutils.lib.gui.misc.ChunkSelectorMap;
import serverutils.lib.io.Bits;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.permission.PermissionAPI;

public class MessageClaimedChunksUpdate extends MessageToClient {

    public int startX, startZ, claimedChunks, loadedChunks, maxClaimedChunks, maxLoadedChunks;
    public Map<Short, ClientClaimedChunks.Team> teams;

    public MessageClaimedChunksUpdate() {}

    public MessageClaimedChunksUpdate(int sx, int sz, EntityPlayer player) {
        startX = sx;
        startZ = sz;

        ForgePlayer p = Universe.get().getPlayer(player);
        ServerUtilitiesTeamData teamData = ServerUtilitiesTeamData.get(p.team);

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

        maxClaimedChunks = getMaxClaimedChunks(teamData, p);
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

    private int getMaxClaimedChunks(ServerUtilitiesTeamData teamData, ForgePlayer player) {
        if (teamData.getMaxClaimChunks() < 0) {
            return teamData.getMaxClaimChunks();
        }
        return player.hasPermission(ServerUtilitiesPermissions.CLAIMS_BYPASS_LIMITS) ? Integer.MAX_VALUE
                : teamData.getMaxClaimChunks();
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.CLAIMS;
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
