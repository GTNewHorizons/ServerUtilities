package serverutils.integration.navigator.journeymap;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.navigator.api.journeymap.render.JMInteractableLayerRenderer;
import com.gtnewhorizons.navigator.api.model.layers.InteractableLayerManager;
import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;
import com.gtnewhorizons.navigator.api.model.steps.RenderStep;

import serverutils.integration.navigator.ClaimsLocation;
import serverutils.integration.navigator.NavigatorIntegration;

public class JMClaimsRenderer extends JMInteractableLayerRenderer {

    public JMClaimsRenderer(InteractableLayerManager manager) {
        super(manager);
    }

    @Override
    protected List<? extends RenderStep> generateRenderSteps(List<? extends ILocationProvider> visibleElements) {
        final List<JMRenderStep> drawSteps = new ArrayList<>();
        visibleElements.stream().map(element -> (ClaimsLocation) element)
                .forEach(location -> drawSteps.add(new JMRenderStep(location)));
        return drawSteps;
    }

    @Override
    public boolean onClickOutsideRenderStep(boolean isDoubleClick, int mouseX, int mouseY, int blockX, int blockZ) {
        if (isDoubleClick) {
            NavigatorIntegration.claimChunk(blockX, blockZ);
            return true;
        }
        return false;
    }
}
