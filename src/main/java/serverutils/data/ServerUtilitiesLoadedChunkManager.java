package serverutils.data;

import static serverutils.ServerUtilitiesPermissions.CHUNKLOADER_LOAD_OFFLINE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.TeamType;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.ServerUtils;

public class ServerUtilitiesLoadedChunkManager implements ForgeChunkManager.LoadingCallback {

    public static final ServerUtilitiesLoadedChunkManager INSTANCE = new ServerUtilitiesLoadedChunkManager();

    public final Map<TicketKey, ForgeChunkManager.Ticket> ticketMap = new HashMap<>();
    private final Map<ChunkDimPos, ForgeChunkManager.Ticket> chunkTickets = new HashMap<>();

    public void clear() {
        ticketMap.clear();
        chunkTickets.clear();
    }

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        final int dim = world.provider.dimensionId;
        for (ForgeChunkManager.Ticket ticket : tickets) {
            TicketKey key = new TicketKey(dim, ticket.getModData().getString("Team"));

            if (!key.teamId.isEmpty()) {
                if (!ticket.getChunkList().isEmpty()) {
                    ticketMap.put(key, ticket);
                }

                for (ChunkCoordIntPair pos : ticket.getChunkList()) {
                    chunkTickets.put(new ChunkDimPos(pos, key.dimension), ticket);
                    ForgeChunkManager.forceChunk(ticket, pos);
                }
            }
        }
    }

    @Nullable
    public ForgeChunkManager.Ticket requestTicket(MinecraftServer server, TicketKey key) {
        ForgeChunkManager.Ticket ticket = ticketMap.get(key);

        if (ticket == null && DimensionManager.isDimensionRegistered(key.dimension)) {
            WorldServer worldServer = server.worldServerForDimension(key.dimension);
            ticket = ForgeChunkManager.requestTicket(ServerUtilities.INST, worldServer, ForgeChunkManager.Type.NORMAL);

            if (ticket != null) {
                ticketMap.put(key, ticket);
                ticket.getModData().setString("Team", key.teamId);
            }
        }

        return ticket;
    }

    public void forceChunk(MinecraftServer server, ClaimedChunk chunk) {
        if (chunk.forced != null && chunk.forced) {
            return;
        }

        ChunkDimPos pos = chunk.getPos();
        ForgeChunkManager.Ticket ticket = requestTicket(server, new TicketKey(pos.dim, chunk.getTeam().getId()));

        try {
            Objects.requireNonNull(ticket);
            ForgeChunkManager.forceChunk(ticket, pos.getChunkPos());
            chunk.forced = true;
            chunkTickets.put(pos, ticket);

            if (ServerUtilitiesConfig.debugging.log_chunkloading) {
                ServerUtilities.LOGGER.info(
                        chunk.getTeam().getTitle().getUnformattedText() + " forced "
                                + pos.posX
                                + ","
                                + pos.posZ
                                + " in "
                                + ServerUtils.getDimensionName(pos.dim).getUnformattedText());
            }
        } catch (Exception ex) {
            if (!DimensionManager.isDimensionRegistered(chunk.getPos().dim)) {
                ServerUtilities.LOGGER.error(
                        "Failed to force chunk " + pos.posX
                                + ","
                                + pos.posZ
                                + " in "
                                + ServerUtils.getDimensionName(pos.dim).getUnformattedText()
                                + " from "
                                + chunk.getTeam().getTitle().getUnformattedText()
                                + ": Dimension "
                                + chunk.getPos().dim
                                + " not registered!");
            } else {
                ServerUtilities.LOGGER.error(
                        "Failed to force chunk " + pos.posX
                                + ","
                                + pos.posZ
                                + " in "
                                + ServerUtils.getDimensionName(pos.dim).getUnformattedText()
                                + " from "
                                + chunk.getTeam().getTitle().getUnformattedText()
                                + ": "
                                + ex);

                if (ServerUtilitiesConfig.debugging.print_more_errors) {
                    ex.printStackTrace();
                }
            }

            if (ServerUtilitiesConfig.world.unload_erroring_chunks) {
                ServerUtilities.LOGGER.warn(
                        "Unloading erroring chunk at " + pos.posX
                                + ","
                                + pos.posZ
                                + " in "
                                + ServerUtils.getDimensionName(pos.dim).getUnformattedText());
                chunk.setLoaded(false);
            }
        }
    }

    public void unforceChunk(ClaimedChunk chunk) {
        if (chunk.forced != null && !chunk.forced) {
            return;
        }

        ChunkDimPos pos = chunk.getPos();
        ForgeChunkManager.Ticket ticket = chunkTickets.get(pos);

        if (ticket == null) {
            return;
        }

        ForgeChunkManager.unforceChunk(ticket, pos.getChunkPos());
        chunkTickets.remove(pos);
        chunk.forced = false;

        if (ticket.getChunkList().isEmpty()) {
            ticketMap.remove(new TicketKey(pos.dim, chunk.getTeam().getId()));
            ForgeChunkManager.releaseTicket(ticket);
        }

        if (ServerUtilitiesConfig.debugging.log_chunkloading) {
            ServerUtilities.LOGGER.info(
                    chunk.getTeam().getTitle().getUnformattedText() + " unforced "
                            + pos.posX
                            + ","
                            + pos.posZ
                            + " in "
                            + ServerUtils.getDimensionName(pos.dim).getUnformattedText());
        }
    }

    public boolean canForceChunks(ForgeTeam team) {
        return team.type == TeamType.SERVER || !team.getOnlineMembers().isEmpty()
                || team.anyMemberHasPermission(CHUNKLOADER_LOAD_OFFLINE);
    }
}
