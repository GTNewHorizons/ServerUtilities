package serverutils.integration.navigator;

import java.util.Collection;
import java.util.Collections;

import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;

import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.client.util.PolygonHelper;
import journeymap.api.v2.common.Context;
import serverutils.ServerUtilities;
import serverutils.lib.icon.Color4I;

final class ClaimsPolygonOverlay {

    private ClaimsPolygonOverlay() {}

    static Collection<?> create(ILocationProvider provider) {
        ClaimsLocation location = (ClaimsLocation) provider;
        Color4I color = location.getTeamColor().getColor();
        if (!location.isLoaded()) color = color.addBrightness(-0.3F);

        ShapeProperties shape = new ShapeProperties().setFillColor(color.rgb()).setFillOpacity(135F / 255F)
                .setStrokeOpacity(0F);
        PolygonOverlay overlay = new PolygonOverlay(
                ServerUtilities.MOD_ID,
                location.getDimensionId(),
                shape,
                PolygonHelper.createChunkPolygon(location.getChunkX(), 64, location.getChunkZ()));
        overlay.setOverlayGroupName(ClaimsLocation.class.getName())
                .setActiveUIs(Context.UI.Fullscreen, Context.UI.Minimap).setDisplayOrder(100);
        return Collections.singleton(overlay);
    }
}
