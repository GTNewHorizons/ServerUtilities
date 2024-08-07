package serverutils.integration.navigator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;

import com.gtnewhorizons.navigator.api.model.SupportedMods;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.layers.LayerRenderer;
import com.gtnewhorizons.navigator.api.model.locations.IWaypointAndLocationProvider;
import com.gtnewhorizons.navigator.api.model.waypoints.Waypoint;
import com.gtnewhorizons.navigator.api.util.Util;

import serverutils.client.gui.ClientClaimedChunks;
import serverutils.integration.navigator.journeymap.JMClaimsRenderer;
import serverutils.integration.navigator.xaero.XaeroClaimsRenderer;
import serverutils.lib.math.ChunkDimPos;
import serverutils.net.MessageJourneyMapRequest;

public class ClaimsLayerManager extends InteractableLayerManager {

    public static final ClaimsLayerManager INSTANCE = new ClaimsLayerManager();
    private int oldMinBlockX = 0;
    private int oldMinBlockZ = 0;
    private int oldMaxBlockX = 0;
    private int oldMaxBlockZ = 0;
    private long lastRequest = 0;

    public ClaimsLayerManager() {
        super(ClaimsButtonManager.INSTANCE);
    }

    @Nullable
    @Override
    protected LayerRenderer addLayerRenderer(InteractableLayerManager manager, SupportedMods mod) {
        return switch (mod) {
            case JourneyMap -> new JMClaimsRenderer(manager);
            case XaeroWorldMap -> new XaeroClaimsRenderer(manager);
            default -> null;
        };
    }

    @Override
    public void setActiveWaypoint(Waypoint waypoint) {}

    @Override
    protected boolean needsRegenerateVisibleElements(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        if (minBlockX != oldMinBlockX || minBlockZ != oldMinBlockZ
                || maxBlockX != oldMaxBlockX
                || maxBlockZ != oldMaxBlockZ) {
            oldMinBlockX = minBlockX;
            oldMinBlockZ = minBlockZ;
            oldMaxBlockX = maxBlockX;
            oldMaxBlockZ = maxBlockZ;
            if (System.currentTimeMillis() - lastRequest >= TimeUnit.SECONDS.toMillis(2)) {
                lastRequest = System.currentTimeMillis();
                new MessageJourneyMapRequest(minBlockX, maxBlockX, minBlockZ, maxBlockZ).sendToServer();
            }
            return true;
        }
        return false;
    }

    @Override
    protected List<? extends IWaypointAndLocationProvider> generateVisibleElements(int minBlockX, int minBlockZ,
            int maxBlockX, int maxBlockZ) {
        int minX = Util.coordBlockToChunk(minBlockX);
        int minZ = Util.coordBlockToChunk(minBlockZ);
        int maxX = Util.coordBlockToChunk(maxBlockX);
        int maxZ = Util.coordBlockToChunk(maxBlockZ);
        final EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;

        ArrayList<ClaimsLocation> locations = new ArrayList<>();

        for (Map.Entry<ChunkDimPos, ClientClaimedChunks.ChunkData> entry : NavigatorIntegration.CLAIMS.entrySet()) {
            ChunkDimPos key = entry.getKey();
            ClientClaimedChunks.ChunkData value = entry.getValue();
            boolean withinRange = key.x >= minX && key.x <= maxX
                    && key.z >= minZ
                    && key.z <= maxZ
                    && key.getDim() == player.dimension;
            if (!withinRange) {
                continue;
            }
            locations.add(new ClaimsLocation(key, value));
        }

        return locations;
    }
}
