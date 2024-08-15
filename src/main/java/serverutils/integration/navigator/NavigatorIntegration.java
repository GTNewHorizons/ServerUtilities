package serverutils.integration.navigator;

import java.util.Collection;
import java.util.Collections;

import net.minecraft.client.Minecraft;
import net.minecraft.world.ChunkCoordIntPair;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.gtnewhorizons.navigator.api.NavigatorApi;
import com.gtnewhorizons.navigator.api.util.ClickPos;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import serverutils.client.gui.ClientClaimedChunks;
import serverutils.net.MessageClaimedChunksModify;
import serverutils.net.MessageClaimedChunksUpdate;
import serverutils.net.MessageJourneyMapRequest;
import serverutils.net.MessageJourneyMapUpdate;

public class NavigatorIntegration {

    public static final Long2ObjectMap<ClientClaimedChunks.ChunkData> CLAIMS = new Long2ObjectOpenHashMap<>();
    public static ClientClaimedChunks.ChunkData OWNTEAM = null;
    public static int maxClaimedChunks, currentClaimedChunks;

    public static void init() {
        NavigatorApi.registerLayerManager(ClaimsLayerManager.INSTANCE);
    }

    public static void updateMap(MessageJourneyMapUpdate message) {
        for (ClientClaimedChunks.Team team : message.teams.values()) {
            for (Long2ObjectMap.Entry<ClientClaimedChunks.ChunkData> pos : team.chunkPos.long2ObjectEntrySet()) {
                long location = pos.getLongKey();
                ClientClaimedChunks.ChunkData newData = pos.getValue();
                ClientClaimedChunks.ChunkData oldData = CLAIMS.get(location);
                if (oldData == null) {
                    CLAIMS.put(location, newData);
                    continue;
                }

                oldData.setLoaded(newData.isLoaded());
                oldData.setTeam(newData.team);
            }

            ClaimsLayerManager.INSTANCE.forceRefresh();

            if (OWNTEAM == null && team.isMember) {
                OWNTEAM = team.chunkPos.values().iterator().next().copy().setLoaded(false);
            }
        }
    }

    public static void addToOwnTeam(int chunkX, int chunkZ) {
        if (OWNTEAM == null) return;
        if (currentClaimedChunks >= maxClaimedChunks) return;
        Minecraft mc = Minecraft.getMinecraft();
        long pos = CoordinatePacker.pack(chunkX, mc.thePlayer.dimension, chunkZ);
        CLAIMS.put(pos, OWNTEAM.copy());
        ClaimsLayerManager.INSTANCE.forceRefresh();
    }

    public static void removeChunk(long location) {
        ClaimsLayerManager.INSTANCE.removeLocation(location);
        CLAIMS.remove(location);
    }

    public static void unclaimChunk(ClaimsLocation location) {
        if (!location.isOwnTeam()) return;
        int chunkX = location.getChunkX();
        int chunkZ = location.getChunkZ();
        int selectionMode = MessageClaimedChunksModify.UNCLAIM;

        Collection<ChunkCoordIntPair> chunks = Collections.singleton(new ChunkCoordIntPair(chunkX, chunkZ));
        new MessageClaimedChunksModify(chunkX, chunkZ, selectionMode, chunks).sendToServer();
        removeChunk(location.toLong());
    }

    public static void onChunkDataUpdate(MessageClaimedChunksUpdate message) {
        maxClaimedChunks = message.maxClaimedChunks;
        currentClaimedChunks = message.claimedChunks;
    }

    public static boolean claimChunk(ClickPos pos) {
        if (pos.getRenderStep() != null || !pos.isDoubleClick()) return false;
        int selectionMode = MessageClaimedChunksModify.CLAIM;
        int chunkX = pos.getChunkX();
        int chunkZ = pos.getChunkZ();
        Collection<ChunkCoordIntPair> chunk = Collections.singleton(new ChunkCoordIntPair(chunkX, chunkZ));
        new MessageClaimedChunksModify(chunkX, chunkZ, selectionMode, chunk).sendToServer();

        if (OWNTEAM == null) {
            new MessageJourneyMapRequest(chunkX, chunkX, chunkZ, chunkZ).sendToServer();
        } else {
            addToOwnTeam(chunkX, chunkZ);
        }
        return true;
    }
}
