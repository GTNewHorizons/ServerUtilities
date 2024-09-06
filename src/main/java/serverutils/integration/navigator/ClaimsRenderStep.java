package serverutils.integration.navigator;

import java.util.List;

import com.gtnewhorizons.navigator.api.model.steps.UniversalInteractableStep;

import serverutils.lib.icon.Color4I;

public class ClaimsRenderStep extends UniversalInteractableStep<ClaimsLocation> {

    public ClaimsRenderStep(ClaimsLocation location) {
        super(location);
    }

    @Override
    public void draw(double x, double y, float drawScale, double zoom) {
        Color4I teamColor = location.getTeamColor().getColor().withAlpha(135);
        if (!location.isLoaded()) {
            teamColor.addBrightness(-0.3f).drawD(x, y, getAdjustedWidth(), getAdjustedHeight());
        } else {
            teamColor.drawD(x, y, getAdjustedWidth(), getAdjustedHeight());
        }
    }

    @Override
    public void getTooltip(List<String> list) {
        list.add(location.getTeamName());

        if (!location.teamHint().isEmpty()) {
            list.add(location.teamHint());
        }
        if (!location.loadedHint().isEmpty()) {
            list.add(location.loadedHint());
        }
        if (location.isOwnTeam()) {
            list.add(location.claimHint());
            list.add(location.toggleLoadHint());
            list.add(location.unclaimHint());
        }
    }

    @Override
    public void onActionKeyPressed() {
        NavigatorIntegration.unclaimChunk(location);
    }
}
