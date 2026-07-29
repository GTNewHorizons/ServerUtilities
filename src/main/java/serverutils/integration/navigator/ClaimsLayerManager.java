package serverutils.integration.navigator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.layers.UniversalInteractableRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.util.Util;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import serverutils.client.gui.ClientClaimedChunks;
import serverutils.lib.math.ChunkDimPos;
import serverutils.net.MessageNavigatorRequest;
import serverutils.net.MessageNavigatorValidateKnown;

public class ClaimsLayerManager extends InteractableLayerManager {

    public static final ClaimsLayerManager INSTANCE = new ClaimsLayerManager();
    private static final int MAX_CHUNKS_TO_VALIDATE = 2000;
    private long lastRequest, lastValidateRequest;

    public ClaimsLayerManager() {
        super(ClaimsButtonManager.INSTANCE);
    }

    @Nullable
    @Override
    protected LayerRenderer addLayerRenderer(InteractableLayerManager manager, SupportedMods mod) {
        UniversalInteractableRenderer renderer = new UniversalInteractableRenderer(manager)
                .withClickAction(NavigatorIntegration::handleMapClick);
        renderer.withRenderStep(location -> new ClaimsRenderStep((ClaimsLocation) location));
        if (Util.isJourneyMapV6Installed()) {
            renderer.withJourneyMapV6Overlays(ClaimsPolygonOverlay::create);
        }
        return renderer;
    }

    @Override
    public void onLayerToggled(boolean toEnabled) {
        super.onLayerToggled(toEnabled);
        if (!toEnabled) {
            NavigatorIntegration.CLAIMS.clear();
            lastRequest = 0;
        }
    }

    @Nullable
    @Override
    protected ILocationProvider generateLocation(int chunkX, int chunkZ, int dim) {
        ClientClaimedChunks.ChunkData data = NavigatorIntegration.CLAIMS
                .get(NavigatorIntegration.mutablePos.set(chunkX, chunkZ, dim));
        if (data == null) return null;
        return new ClaimsLocation(chunkX, chunkZ, dim, data);
    }

    @Override
    protected Collection<? extends ILocationProvider> generateVisibleLocations(int minBlockX, int minBlockZ,
            int maxBlockX, int maxBlockZ, int dimension) {
        int minChunkX = Util.coordBlockToChunk(minBlockX);
        int minChunkZ = Util.coordBlockToChunk(minBlockZ);
        int maxChunkX = Util.coordBlockToChunk(maxBlockX);
        int maxChunkZ = Util.coordBlockToChunk(maxBlockZ);
        List<ClaimsLocation> locations = new ArrayList<>();
        for (Object2ObjectMap.Entry<ChunkDimPos, ClientClaimedChunks.ChunkData> entry : NavigatorIntegration.CLAIMS
                .object2ObjectEntrySet()) {
            ChunkDimPos pos = entry.getKey();
            if (pos.dim == dimension && pos.posX >= minChunkX
                    && pos.posX <= maxChunkX
                    && pos.posZ >= minChunkZ
                    && pos.posZ <= maxChunkZ) {
                locations.add(new ClaimsLocation(pos.posX, pos.posZ, pos.dim, entry.getValue()));
            }
        }
        return locations;
    }

    @Override
    public void onUpdatePre(int minX, int maxX, int minZ, int maxZ) {
        long now = System.currentTimeMillis();
        if (now - lastRequest >= TimeUnit.SECONDS.toMillis(2)) {
            lastRequest = now;
            new MessageNavigatorRequest(minX, maxX, minZ, maxZ).sendToServer();
        }
    }

    @Override
    public void onUpdatePost(int minX, int maxX, int minZ, int maxZ) {
        long now = System.currentTimeMillis();
        if (now - lastValidateRequest >= TimeUnit.SECONDS.toMillis(10)) {
            lastValidateRequest = now;
            Collection<ILocationProvider> visibleLocations = getVisibleLocations();
            if (visibleLocations.isEmpty()) return;

            LongList positions = new LongArrayList();
            visibleLocations.forEach(location -> positions.add(location.toLong()));
            if (visibleLocations.size() <= MAX_CHUNKS_TO_VALIDATE) {
                new MessageNavigatorValidateKnown(positions).sendToServer();
            } else {
                List<LongList> chunks = partitionList(positions);
                for (LongList chunk : chunks) {
                    new MessageNavigatorValidateKnown(chunk).sendToServer();
                }
            }
        }
    }

    private static List<LongList> partitionList(LongList list) {
        List<LongList> chunkList = new ArrayList<>();
        for (int i = 0; i < list.size(); i += MAX_CHUNKS_TO_VALIDATE) {
            int end = Math.min(i + MAX_CHUNKS_TO_VALIDATE, list.size());
            chunkList.add(list.subList(i, end));
        }
        return chunkList;
    }
}
