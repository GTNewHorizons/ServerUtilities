package serverutils.integration.navigator;

import java.util.Collection;
import java.util.Collections;

import net.minecraft.client.Minecraft;
import net.minecraft.world.ChunkCoordIntPair;

import com.gtnewhorizons.navigator.api.NavigatorApi;
import com.gtnewhorizons.navigator.api.util.Util;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import serverutils.client.gui.ClientClaimedChunks;
import serverutils.integration.navigator.journeymap.JMClaimsRenderer;
import serverutils.integration.navigator.xaero.XaeroClaimsRenderer;
import serverutils.lib.math.ChunkDimPos;
import serverutils.net.MessageClaimedChunksModify;
import serverutils.net.MessageClaimedChunksUpdate;
import serverutils.net.MessageJourneyMapRequest;
import serverutils.net.MessageJourneyMapUpdate;

public class NavigatorIntegration {

    public static final Object2ObjectMap<ChunkDimPos, ClientClaimedChunks.ChunkData> CLAIMS = new Object2ObjectOpenHashMap<>();
    public static ClientClaimedChunks.ChunkData OWNTEAM = null;
    public static int maxClaimedChunks, currentClaimedChunks;

    public static void init() {
        NavigatorApi.registerLayerManager(ClaimsLayerManager.INSTANCE);
        NavigatorApi.registerButtonManager(ClaimsButtonManager.INSTANCE);
        if (Util.isJourneyMapInstalled()) {
            NavigatorApi.registerLayerRenderer(JMClaimsRenderer.INSTANCE);
        }

        if (Util.isXaerosWorldMapInstalled()) {
            NavigatorApi.registerLayerRenderer(XaeroClaimsRenderer.INSTANCE);
        }
    }

    public static void updateMap(MessageJourneyMapUpdate message) {
        for (ClientClaimedChunks.Team team : message.teams.values()) {
            CLAIMS.putAll(team.chunkPos);
            if (OWNTEAM == null && team.isMember) {
                OWNTEAM = team.chunkPos.values().iterator().next().copy().setLoaded(false);
            }
        }
        ClaimsLayerManager.INSTANCE.forceRefresh();
    }

    public static void addToOwnTeam(ChunkDimPos pos) {
        if (OWNTEAM == null) return;
        if (currentClaimedChunks >= maxClaimedChunks) return;

        CLAIMS.put(pos, OWNTEAM);
        ClaimsLayerManager.INSTANCE.forceRefresh();
    }

    public static void onChunkDataUpdate(MessageClaimedChunksUpdate message) {
        maxClaimedChunks = message.maxClaimedChunks;
        currentClaimedChunks = message.claimedChunks;
    }

    public static void claimChunk(int blockX, int blockZ) {
        Minecraft mc = Minecraft.getMinecraft();
        int selectionMode = MessageClaimedChunksModify.CLAIM;
        int chunkX = Util.coordBlockToChunk(blockX);
        int chunkZ = Util.coordBlockToChunk(blockZ);
        Collection<ChunkCoordIntPair> chunk = Collections.singleton(new ChunkCoordIntPair(chunkX, chunkZ));
        ChunkDimPos chunkDimPos = new ChunkDimPos(chunkX, chunkZ, mc.thePlayer.dimension);
        new MessageClaimedChunksModify(chunkX, chunkZ, selectionMode, chunk).sendToServer();

        if (OWNTEAM == null) {
            new MessageJourneyMapRequest(blockX, blockX, blockZ, blockZ).sendToServer();
        } else {
            addToOwnTeam(chunkDimPos);
        }
    }
}
