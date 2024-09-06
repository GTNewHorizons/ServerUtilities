package serverutils.net;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import serverutils.ServerUtilitiesPermissions;
import serverutils.client.gui.ClientClaimedChunks;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.integration.navigator.NavigatorIntegration;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;
import serverutils.lib.io.Bits;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.permission.PermissionAPI;

public class MessageNavigatorUpdate extends MessageToClient {

    public Short2ObjectMap<ClientClaimedChunks.Team> teams;

    public MessageNavigatorUpdate() {}

    public MessageNavigatorUpdate(int minX, int maxX, int minZ, int maxZ, EntityPlayer player) {
        ForgePlayer p = Universe.get().getPlayer(player);
        boolean canSeeChunkInfo = PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.CLAIMS_OTHER_SEE_INFO);
        boolean canSeeOtherJourneymap = PermissionAPI
                .hasPermission(player, ServerUtilitiesPermissions.CLAIMS_JOURNEYMAP_OTHER);
        teams = new Short2ObjectOpenHashMap<>();

        ChunkDimPos pos = new ChunkDimPos();
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                ClaimedChunk chunk = ClaimedChunks.instance.getChunk(pos.set(chunkX, chunkZ, player.dimension));
                if (chunk != null) {
                    ForgeTeam chunkTeam = chunk.getTeam();

                    if (!chunkTeam.isValid()) {
                        continue;
                    }

                    if (!canSeeOtherJourneymap && !p.team.equalsTeam(chunkTeam)) {
                        continue;
                    }

                    ClientClaimedChunks.Team team = teams.get(chunkTeam.getUID());
                    boolean member = chunkTeam.isMember(p);
                    int flags = 0;

                    if (team == null) {
                        team = new ClientClaimedChunks.Team(chunkTeam.getUID());
                        team.color = chunkTeam.getColor();
                        team.nameComponent = chunkTeam.getTitle();
                        team.isAlly = chunkTeam.isAlly(p);
                        team.isMember = member;
                        teams.put(chunkTeam.getUID(), team);
                    }

                    if (canSeeChunkInfo || member) {
                        if (chunk.isLoaded()) {
                            flags = Bits.setFlag(flags, ClientClaimedChunks.ChunkData.LOADED, true);
                        }
                    }

                    team.chunkPos.put(chunk.getPos(), new ClientClaimedChunks.ChunkData(team, flags));
                }
            }
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.CLAIMS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeCollection(teams.values(), ClientClaimedChunks.Team.SERIALIZER);
    }

    @Override
    public void readData(DataIn data) {
        teams = new Short2ObjectOpenHashMap<>();
        for (ClientClaimedChunks.Team team : data.readCollection(ClientClaimedChunks.Team.DESERIALIZER)) {
            teams.put(team.uid, team);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        NavigatorIntegration.updateMap(this);
    }
}
