package serverutils.integration.vp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;

import com.sinthoras.visualprospecting.Utils;
import com.sinthoras.visualprospecting.integration.model.layers.WaypointProviderManager;
import com.sinthoras.visualprospecting.integration.model.locations.IWaypointAndLocationProvider;

import serverutils.client.gui.ClientClaimedChunks;
import serverutils.lib.math.ChunkDimPos;
import serverutils.net.MessageJourneyMapRequest;

public class VPLayerManager extends WaypointProviderManager {

    public static final VPLayerManager INSTANCE = new VPLayerManager();
    private int oldMinBlockX = 0;
    private int oldMinBlockZ = 0;
    private int oldMaxBlockX = 0;
    private int oldMaxBlockZ = 0;
    private long lastRequest = 0;

    public VPLayerManager() {
        super(VPButtonManager.INSTANCE);
    }

    @Override
    protected boolean needsRegenerateVisibleElements(int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
        if (minBlockX != oldMinBlockX || minBlockZ != oldMinBlockZ
                || maxBlockX != oldMaxBlockX
                || maxBlockZ != oldMaxBlockZ) {
            oldMinBlockX = minBlockX;
            oldMinBlockZ = minBlockZ;
            oldMaxBlockX = maxBlockX;
            oldMaxBlockZ = maxBlockZ;
            if (System.currentTimeMillis() - lastRequest >= TimeUnit.SECONDS.toMillis(10)) {
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
        int minX = Utils.coordBlockToChunk(minBlockX);
        int minZ = Utils.coordBlockToChunk(minBlockZ);
        int maxX = Utils.coordBlockToChunk(maxBlockX);
        int maxZ = Utils.coordBlockToChunk(maxBlockZ);
        final EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;

        ArrayList<VPClaimsLocation> locations = new ArrayList<>();

        for (Map.Entry<ChunkDimPos, ClientClaimedChunks.ChunkData> entry : VPIntegration.CLAIMS.entrySet()) {
            ChunkDimPos key = entry.getKey();
            ClientClaimedChunks.ChunkData value = entry.getValue();
            boolean withinRange = key.posX >= minX && key.posX <= maxX
                    && key.posZ >= minZ
                    && key.posZ <= maxZ
                    && key.dim == player.dimension;
            if (!withinRange) {
                continue;
            }
            locations.add(new VPClaimsLocation(key, value));
        }

        return locations;
    }
}
