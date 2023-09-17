package serverutils.utils.mod.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

import com.google.common.collect.MapMaker;

import serverutils.lib.EventBusHelper;
import serverutils.lib.LMDimUtils;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.utils.mod.ServerUtilities;
import serverutils.utils.mod.ServerUtilitiesFinals;
import serverutils.utils.mod.config.ServerUtilitiesConfigChunkloading;
import serverutils.utils.world.LMPlayerServer;
import serverutils.utils.world.LMWorldServer;
import serverutils.utils.world.claims.ClaimedChunk;

public class ServerUtilitiesChunkEventHandler
        implements ForgeChunkManager.LoadingCallback, ForgeChunkManager.OrderedLoadingCallback {

    public static final ServerUtilitiesChunkEventHandler instance = new ServerUtilitiesChunkEventHandler();
    private final Map<World, Map<Integer, ForgeChunkManager.Ticket>> table = new MapMaker().weakKeys().makeMap();
    private static final String PLAYER_ID_TAG = "PID";

    public void init() {
        if (!ForgeChunkManager.getConfig().hasCategory(ServerUtilitiesFinals.MOD_ID)) {
            ForgeChunkManager.getConfig().get(ServerUtilitiesFinals.MOD_ID, "maximumTicketCount", 2000).setMinValue(0);
            ForgeChunkManager.getConfig().get(ServerUtilitiesFinals.MOD_ID, "maximumChunksPerTicket", 30000)
                    .setMinValue(0);
            ForgeChunkManager.getConfig().save();
        }

        EventBusHelper.register(this);
        ForgeChunkManager.setForcedChunkLoadingCallback(ServerUtilities.inst, this);
    }

    private ForgeChunkManager.Ticket request(World w, LMPlayerServer player) {
        if (w == null || player == null) return null;

        Integer playerID = Integer.valueOf(player.getPlayerID());

        Map<Integer, ForgeChunkManager.Ticket> map = table.get(w);
        ForgeChunkManager.Ticket t = (map == null) ? null : map.get(playerID);

        if (t == null) {
            t = ForgeChunkManager.requestTicket(ServerUtilities.inst, w, ForgeChunkManager.Type.NORMAL);
            if (t == null) return null;
            else {
                t.getModData().setInteger(PLAYER_ID_TAG, playerID);

                if (map == null) {
                    map = new HashMap<>();
                    table.put(w, map);
                }

                map.put(playerID, t);
            }
        }

        return t;
    }

    public List<ForgeChunkManager.Ticket> ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world,
            int maxTicketCount) {
        table.remove(world);
        List<ForgeChunkManager.Ticket> tickets1 = new ArrayList<>();
        if (tickets.isEmpty() || !ServerUtilitiesConfigChunkloading.enabled.getAsBoolean()) return tickets1;
        Map<Integer, ForgeChunkManager.Ticket> map = new HashMap<>();

        for (ForgeChunkManager.Ticket t : tickets) {
            int playerID = t.getModData().getInteger(PLAYER_ID_TAG);

            if (playerID > 0) {
                map.put(playerID, t);
                tickets1.add(t);
            }
        }

        table.put(world, map);
        return tickets1;
    }

    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        for (ForgeChunkManager.Ticket t : tickets) {
            int playerID = t.getModData().getInteger(PLAYER_ID_TAG);

            if (playerID > 0) {
                List<ClaimedChunk> chunks = LMWorldServer.inst.claimedChunks
                        .getChunks(LMWorldServer.inst.getPlayer(playerID), world.provider.dimensionId);

                if (chunks != null && !chunks.isEmpty()) for (ClaimedChunk c : chunks) {
                    if (c.isChunkloaded) {
                        ForgeChunkManager.forceChunk(t, c.getPos());
                    }
                }
            }
        }

        // force chunks //
        markDirty(world);
    }

    public void markDirty(World w) {
        if (LMWorldServer.inst == null || ServerUtilitiesLib.getServerWorld() == null) return;
        if (w != null) markDirty0(w);

        if (!table.isEmpty()) {
            World[] worlds = table.keySet().toArray(new World[table.size()]);
            for (World w1 : worlds) markDirty0(w1);
        }
    }

    private void markDirty0(World w) {
        /*
         * int total = 0; int totalLoaded = 0; int markedLoaded = 0; int loaded = 0; int unloaded = 0;
         */

        Map<Long, ClaimedChunk> chunksMap = LMWorldServer.inst.claimedChunks.chunks.get(w.provider.dimensionId);

        double max = ServerUtilitiesConfigChunkloading.enabled.getAsBoolean()
                ? ServerUtilitiesConfigChunkloading.max_player_offline_hours.getAsDouble()
                : -2D;

        if (chunksMap != null) for (ClaimedChunk c : chunksMap.values()) {
            // total++;

            boolean isLoaded = c.isChunkloaded;

            if (c.isChunkloaded) {
                LMPlayerServer p = c.getOwnerS();
                if (p == null) isLoaded = false;
                else {
                    if (max == -2D) isLoaded = false;
                    else if (max == -1D) isLoaded = true;
                    else if (max == 0D) isLoaded = p.isOnline();
                    else if (max > 0D) {
                        if (!p.isOnline()) {
                            if (max > 0D && p.stats.getLastSeenDeltaInHours(p) > max) {
                                isLoaded = false;
                                // if(c.isForced) unloaded.add(p.getPlayerID());
                            }
                        }
                    }
                }
            }

            // if(isLoaded) totalLoaded++;
            // if(c.isChunkloaded) markedLoaded++;

            if (c.isForced != isLoaded) {
                ForgeChunkManager.Ticket ticket = request(LMDimUtils.getWorld(c.dim), c.getOwnerS());

                if (ticket != null) {
                    if (isLoaded) {
                        ForgeChunkManager.forceChunk(ticket, c.getPos());
                        // loaded++;
                    } else {
                        ForgeChunkManager.unforceChunk(ticket, c.getPos());
                        // unloaded++;
                    }

                    c.isForced = isLoaded;
                }
            }
        }

        // ServerUtilitiesLibrary.dev_logger.info("Total: " + total + ", Loaded: " + totalLoaded + "/" + markedLoaded +
        // ", DLoaded: " +
        // loaded + ", DUnloaded: " + unloaded);
    }

    private void releaseTicket(ForgeChunkManager.Ticket t) {
        if (t.getModData().hasKey(PLAYER_ID_TAG)) {
            Map<Integer, ForgeChunkManager.Ticket> map = table.get(t.world);

            if (map != null) {
                map.remove(t.getModData().getInteger(PLAYER_ID_TAG));

                if (map.isEmpty()) {
                    table.remove(t.world);
                }
            }
        }

        ForgeChunkManager.releaseTicket(t);
    }

    public void clear() {
        table.clear();
    }
}
