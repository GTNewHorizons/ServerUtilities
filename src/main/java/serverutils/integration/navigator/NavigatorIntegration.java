package serverutils.integration.navigator;

import java.util.Collection;
import java.util.Collections;

import net.minecraft.client.Minecraft;
import net.minecraft.world.ChunkCoordIntPair;

import com.gtnewhorizons.navigator.api.NavigatorApi;
import com.gtnewhorizons.navigator.api.util.ClickPos;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import serverutils.client.gui.ClientClaimedChunks;
import serverutils.lib.math.ChunkDimPos;
import serverutils.net.MessageClaimedChunksModify;
import serverutils.net.MessageClaimedChunksUpdate;
import serverutils.net.MessageNavigatorRequest;
import serverutils.net.MessageNavigatorUpdate;

public class NavigatorIntegration {

    public static final Object2ObjectMap<ChunkDimPos, ClientClaimedChunks.ChunkData> CLAIMS = new Object2ObjectOpenHashMap<>();
    public static ClientClaimedChunks.ChunkData OWNTEAM = null;
    public static int maxClaimedChunks, currentClaimedChunks;
    static final ChunkDimPos mutablePos = new ChunkDimPos();

    public static void init() {
        NavigatorApi.registerLayerManager(ClaimsLayerManager.INSTANCE);
    }

    public static void updateMap(MessageNavigatorUpdate message) {
        for (ClientClaimedChunks.Team team : message.teams.values()) {
            for (Object2ObjectMap.Entry<ChunkDimPos, ClientClaimedChunks.ChunkData> pos : team.chunkPos
                    .object2ObjectEntrySet()) {
                ChunkDimPos location = pos.getKey();
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
        CLAIMS.put(new ChunkDimPos(chunkX, chunkZ, mc.thePlayer.dimension), OWNTEAM.copy());
        ClaimsLayerManager.INSTANCE.forceRefresh();
    }

    public static void removeChunk(int chunkX, int chunkZ) {
        int dim = Minecraft.getMinecraft().thePlayer.dimension;
        ClaimsLayerManager.INSTANCE.removeLocation(chunkX, chunkZ);
        CLAIMS.remove(mutablePos.set(chunkX, chunkZ, dim));
    }

    public static void unclaimChunk(ClaimsLocation location) {
        if (!location.isOwnTeam()) return;
        int chunkX = location.getChunkX();
        int chunkZ = location.getChunkZ();
        int selectionMode = MessageClaimedChunksModify.UNCLAIM;

        Collection<ChunkCoordIntPair> chunks = Collections.singleton(new ChunkCoordIntPair(chunkX, chunkZ));
        new MessageClaimedChunksModify(chunkX, chunkZ, selectionMode, chunks).sendToServer();
        removeChunk(location.getChunkX(), location.getChunkZ());
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
            new MessageNavigatorRequest(chunkX, chunkX, chunkZ, chunkZ).sendToServer();
        } else {
            addToOwnTeam(chunkX, chunkZ);
        }
        return true;
    }
}
