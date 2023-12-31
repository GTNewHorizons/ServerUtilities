package serverutils.integration.vp;

import com.sinthoras.visualprospecting.VisualProspecting_API;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import serverutils.client.gui.ClientClaimedChunks;
import serverutils.integration.vp.journeymap.VPClaimsRenderer;
import serverutils.integration.vp.journeymap.VPLayerButton;
import serverutils.lib.math.ChunkDimPos;
import serverutils.net.MessageJourneyMapUpdate;

public class VPIntegration {

    public static final Object2ObjectMap<ChunkDimPos, ClientClaimedChunks.ChunkData> CLAIMS = new Object2ObjectOpenHashMap<>();

    public static void init() {
        VisualProspecting_API.LogicalClient.registerCustomButtonManager(VPButtonManager.INSTANCE);
        VisualProspecting_API.LogicalClient.registerJourneyMapButton(VPLayerButton.INSTANCE);
        VisualProspecting_API.LogicalClient.registerCustomLayer(VPLayerManager.INSTANCE);
        VisualProspecting_API.LogicalClient.registerJourneyMapRenderer(VPClaimsRenderer.INSTANCE);
    }

    public static void updateMap(MessageJourneyMapUpdate message) {
        for (ClientClaimedChunks.Team team : message.teams.values()) {
            CLAIMS.putAll(team.chunkPos);
        }
        VPLayerManager.INSTANCE.forceRefresh();
    }
}
