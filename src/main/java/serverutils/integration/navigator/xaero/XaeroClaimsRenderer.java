package serverutils.integration.navigator.xaero;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.model.steps.RenderStep;
import com.gtnewhorizons.navigator.api.xaero.renderers.XaeroInteractableLayerRenderer;

import serverutils.integration.navigator.ClaimsLayerManager;
import serverutils.integration.navigator.ClaimsLocation;
import serverutils.integration.navigator.NavigatorIntegration;

public class XaeroClaimsRenderer extends XaeroInteractableLayerRenderer {

    public static final XaeroClaimsRenderer INSTANCE = new XaeroClaimsRenderer();

    public XaeroClaimsRenderer() {
        super(ClaimsLayerManager.INSTANCE);
    }

    @Override
    protected List<? extends RenderStep> generateRenderSteps(List<? extends ILocationProvider> visibleElements) {
        final List<XaeroRenderStep> renderSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (ClaimsLocation) element)
                .forEach(location -> renderSteps.add(new XaeroRenderStep(location)));
        return renderSteps;
    }

    @Override
    public void onClickOutsideRenderStep(boolean isDoubleClick, int mouseX, int mouseY, int mouseBlockX,
            int mouseBlockZ) {
        if (isDoubleClick) {
            NavigatorIntegration.claimChunk(mouseBlockX, mouseBlockZ);
        }
    }
}
