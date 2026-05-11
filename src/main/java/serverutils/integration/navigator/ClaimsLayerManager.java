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
import com.gtnewhorizons.navigator.api.model.locations.IWaypointAndLocationProvider;
import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import serverutils.client.gui.ClientClaimedChunks;
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
        return new UniversalInteractableRenderer(manager).withClickAction(NavigatorIntegration::claimChunk)
                .withRenderStep(location -> new ClaimsRenderStep((ClaimsLocation) location));
    }

    @Override
    public void setActiveWaypoint(Waypoint waypoint) {}

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
            Collection<IWaypointAndLocationProvider> visibleLocations = getVisibleLocations();
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
