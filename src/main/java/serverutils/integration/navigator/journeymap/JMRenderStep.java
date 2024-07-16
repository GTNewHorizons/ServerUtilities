package serverutils.integration.navigator.journeymap;

import java.awt.geom.Point2D;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;

import com.gtnewhorizons.navigator.api.NavigatorApi;
import com.gtnewhorizons.navigator.api.journeymap.drawsteps.JMInteractableStep;
import com.gtnewhorizons.navigator.api.model.locations.IWaypointAndLocationProvider;

import journeymap.client.render.map.GridRenderer;
import serverutils.integration.navigator.ClaimsLocation;
import serverutils.lib.icon.Color4I;

public class JMRenderStep implements JMInteractableStep {

    private final ClaimsLocation location;
    private double topX = 0;
    private double topY = 0;
    private double chunkSize = 0;

    public JMRenderStep(ClaimsLocation location) {
        this.location = location;
    }

    @Override
    public void draw(double draggedPixelX, double draggedPixelY, GridRenderer gridRenderer, float drawScale,
            double fontScale, double rotation) {
        final int zoom = gridRenderer.getZoom();
        double blockSize = Math.pow(2, zoom);
        final Point2D.Double blockAsPixel = gridRenderer
                .getBlockPixelInGrid(location.getBlockX(), location.getBlockZ());
        final Point2D.Double pixel = new Point2D.Double(
                blockAsPixel.getX() + draggedPixelX,
                blockAsPixel.getY() + draggedPixelY);

        chunkSize = blockSize * NavigatorApi.CHUNK_WIDTH;
        topX = pixel.getX();
        topY = pixel.getY();

        Color4I teamColor = location.getTeamColor().getColor().withAlpha(135);
        if (!location.isLoaded()) {
            teamColor.addBrightness(-0.3f)
                    .draw((int) pixel.getX(), (int) pixel.getY(), (int) chunkSize, (int) chunkSize);
        } else {
            teamColor.draw((int) pixel.getX(), (int) pixel.getY(), (int) chunkSize, (int) chunkSize);
        }
    }

    @Override
    public void getTooltip(List<String> tooltip) {
        tooltip.add(location.getTeamName());

        if (!location.teamHint().isEmpty()) {
            tooltip.add(location.teamHint());
        }
        if (!location.loadedHint().isEmpty()) {
            tooltip.add(location.loadedHint());
        }
        if (location.getOwnTeam()) {
            tooltip.add(location.claimHint());
            tooltip.add(location.toggleLoadHint());
            tooltip.add(location.unclaimHint());
        }
    }

    @Override
    public void drawCustomTooltip(FontRenderer fontRenderer, int mouseX, int mouseY, int displayWidth,
            int displayHeight) {}

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= topX && mouseX <= topX + chunkSize && mouseY >= topY && mouseY <= topY + chunkSize;
    }

    @Override
    public void onActionKeyPressed() {
        location.removeClaim();
    }

    @Override
    public IWaypointAndLocationProvider getLocationProvider() {
        return location;
    }
}
