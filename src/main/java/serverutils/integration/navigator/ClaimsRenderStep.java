package serverutils.integration.navigator;

import java.util.List;

import com.gtnewhorizons.navigator.api.model.steps.UniversalLocationInteractableStep;
import com.gtnewhorizons.navigator.api.util.Util;

import serverutils.lib.icon.Color4I;

public class ClaimsRenderStep extends UniversalLocationInteractableStep<ClaimsLocation> {

    static final Color4I LOADED_BORDER = Color4I.rgb(255, 80, 80).withAlpha(230);

    public ClaimsRenderStep(ClaimsLocation location) {
        super(location);
    }

    @Override
    public void preRender(double x, double y, float drawScale, double zoom) {
        setOffset(isJourneyMap ? -blockSize / 2D : 0D);
    }

    @Override
    public void draw(double x, double y, float drawScale, double zoom) {
        double width = getAdjustedWidth();
        double height = getAdjustedHeight();
        if (!isJourneyMap || !Util.isJourneyMapV6Installed()) {
            Color4I teamColor = location.getTeamColor().getColor().withAlpha(135);
            if (!location.isLoaded()) teamColor = teamColor.addBrightness(-0.3F);
            teamColor.drawD(x, y, width, height);
        }
        if (!location.isLoaded()) return;

        double border = Math.min(2D, Math.min(width, height) / 2D);
        boolean north = location.hasLoadedNeighbor(0, -1);
        boolean south = location.hasLoadedNeighbor(0, 1);
        boolean west = location.hasLoadedNeighbor(-1, 0);
        boolean east = location.hasLoadedNeighbor(1, 0);
        double horizontalX = x - (west ? border : 0D);
        double horizontalWidth = width + (west ? border : 0D) + (east ? border : 0D);
        double verticalY = y - (north ? border : 0D);
        double verticalHeight = height + (north ? border : 0D) + (south ? border : 0D);
        if (!north) LOADED_BORDER.drawD(horizontalX, y, horizontalWidth, border);
        if (!south) {
            LOADED_BORDER.drawD(horizontalX, y + height - border, horizontalWidth, border);
        }
        if (!west) LOADED_BORDER.drawD(x, verticalY, border, verticalHeight);
        if (!east) {
            LOADED_BORDER.drawD(x + width - border, verticalY, border, verticalHeight);
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
