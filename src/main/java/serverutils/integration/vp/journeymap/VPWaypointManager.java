package serverutils.integration.vp.journeymap;

import com.sinthoras.visualprospecting.integration.journeymap.waypoints.WaypointManager;

import serverutils.integration.vp.VPLayerManager;

public class VPWaypointManager extends WaypointManager {

    // Currently unused
    public static final VPWaypointManager INSTANCE = new VPWaypointManager();

    public VPWaypointManager() {
        super(VPLayerManager.INSTANCE);
    }
}
