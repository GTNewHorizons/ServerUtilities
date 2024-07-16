package serverutils.integration.navigator.xaero;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.navigator.api.NavigatorApi;
import com.gtnewhorizons.navigator.api.model.locations.IWaypointAndLocationProvider;
import com.gtnewhorizons.navigator.api.xaero.rendersteps.XaeroInteractableStep;

import serverutils.integration.navigator.ClaimsLocation;
import serverutils.lib.icon.Color4I;

public class XaeroRenderStep implements XaeroInteractableStep {

    private final ClaimsLocation location;
    private double iconX;
    private double iconY;
    private double chunkSize = 0;

    public XaeroRenderStep(ClaimsLocation location) {
        this.location = location;
    }

    @Override
    public void draw(@Nullable GuiScreen gui, double cameraX, double cameraZ, double scale) {
        final double scaleForGui = Math.max(1, scale);
        iconX = (location.getBlockX() - cameraX) * scaleForGui;
        iconY = (location.getBlockZ() - cameraZ) * scaleForGui;

        chunkSize = scaleForGui * NavigatorApi.CHUNK_WIDTH;

        GL11.glPushMatrix();
        GL11.glTranslated(location.getBlockX() - 0.5 - cameraX, location.getBlockZ() - 0.5 - cameraZ, 0);
        GL11.glScaled(1 / scaleForGui, 1 / scaleForGui, 1);

        Color4I teamColor = location.getTeamColor().getColor().withAlpha(135);
        if (!location.isLoaded()) {
            teamColor.addBrightness(-0.3f).draw(0, 0, (int) chunkSize, (int) chunkSize);
        } else {
            teamColor.draw(0, 0, (int) chunkSize, (int) chunkSize);
        }
        GL11.glPopMatrix();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY, double scale) {
        final double scaleForGui = Math.max(1, scale);
        mouseX = mouseX * scaleForGui;
        mouseY = mouseY * scaleForGui;
        return mouseX >= iconX && mouseY >= iconY && mouseX <= iconX + chunkSize && mouseY <= iconY + chunkSize;
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
        if (location.getOwnTeam()) {
            list.add(location.claimHint());
            list.add(location.toggleLoadHint());
            list.add(location.unclaimHint());
        }
    }

    @Override
    public void drawCustomTooltip(GuiScreen gui, double mouseX, double mouseY, double scale, int scaleAdj) {}

    @Override
    public void onActionButton() {
        location.removeClaim();
    }

    @Override
    public IWaypointAndLocationProvider getLocationProvider() {
        return location;
    }
}
