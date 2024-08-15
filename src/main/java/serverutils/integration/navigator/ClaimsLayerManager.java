package serverutils.integration.navigator;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.layers.UniversalInteractableRenderer;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;

import serverutils.client.gui.ClientClaimedChunks;
import serverutils.net.MessageJourneyMapRequest;

public class ClaimsLayerManager extends InteractableLayerManager {

    public static final ClaimsLayerManager INSTANCE = new ClaimsLayerManager();
    private long lastRequest = 0;

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
    public void onLayerToggled(boolean state) {
        super.onLayerToggled(state);
        if (!state) {
            NavigatorIntegration.CLAIMS.clear();
            lastRequest = 0;
        }
    }

    @Nullable
    @Override
    protected ILocationProvider generateLocation(long packedChunk) {
        ClientClaimedChunks.ChunkData data = NavigatorIntegration.CLAIMS.get(packedChunk);
        if (data == null) return null;
        return new ClaimsLocation(packedChunk, data);
    }

    @Override
    public void onUpdatePre(int minX, int maxX, int minZ, int maxZ) {
        if (System.currentTimeMillis() - lastRequest >= TimeUnit.SECONDS.toMillis(2)) {
            lastRequest = System.currentTimeMillis();
            new MessageJourneyMapRequest(minX, maxX, minZ, maxZ).sendToServer();
        }
    }
}
