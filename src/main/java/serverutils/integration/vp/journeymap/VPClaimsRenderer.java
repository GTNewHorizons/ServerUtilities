package serverutils.integration.vp.journeymap;

import java.util.ArrayList;
import java.util.List;

import com.sinthoras.visualprospecting.integration.journeymap.drawsteps.ClickableDrawStep;
import com.sinthoras.visualprospecting.integration.journeymap.render.WaypointProviderLayerRenderer;
import com.sinthoras.visualprospecting.integration.model.locations.ILocationProvider;

import serverutils.integration.vp.VPClaimsLocation;
import serverutils.integration.vp.VPLayerManager;

public class VPClaimsRenderer extends WaypointProviderLayerRenderer {

    public static final VPClaimsRenderer INSTANCE = new VPClaimsRenderer();

    public VPClaimsRenderer() {
        super(VPLayerManager.INSTANCE);
    }

    @Override
    protected List<? extends ClickableDrawStep> mapLocationProviderToDrawStep(
            List<? extends ILocationProvider> visibleElements) {
        final List<VPDrawStep> drawSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (VPClaimsLocation) element)
                .forEach(location -> drawSteps.add(new VPDrawStep(location)));
        return drawSteps;
    }
}
