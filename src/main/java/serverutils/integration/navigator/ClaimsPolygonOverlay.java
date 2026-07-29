package serverutils.integration.navigator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gtnewhorizons.navigator.api.model.locations.ILocationProvider;

import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.client.util.PolygonHelper;
import journeymap.api.v2.common.Context;
import journeymap.api.v2.common.util.BlockPos;
import serverutils.ServerUtilities;
import serverutils.lib.icon.Color4I;

final class ClaimsPolygonOverlay {

    private ClaimsPolygonOverlay() {}

    static Collection<?> create(ILocationProvider provider) {
        ClaimsLocation location = (ClaimsLocation) provider;
        Color4I color = location.getTeamColor().getColor();
        if (!location.isLoaded()) color = color.addBrightness(-0.3F);

        ShapeProperties fill = new ShapeProperties().setFillColor(color.rgb()).setFillOpacity(135F / 255F)
                .setStrokeOpacity(0F);
        List<PolygonOverlay> overlays = new ArrayList<>();
        overlays.add(
                createOverlay(
                        location,
                        fill,
                        PolygonHelper.createChunkPolygon(location.getChunkX(), 64, location.getChunkZ()),
                        100,
                        Context.UI.Fullscreen,
                        Context.UI.Minimap));
        if (!location.isLoaded()) return overlays;

        int x = location.getChunkX() << 4;
        int z = location.getChunkZ() << 4;
        boolean north = location.hasLoadedNeighbor(0, -1);
        boolean south = location.hasLoadedNeighbor(0, 1);
        boolean west = location.hasLoadedNeighbor(-1, 0);
        boolean east = location.hasLoadedNeighbor(1, 0);
        int horizontalMinX = x - (west ? 1 : 0);
        int horizontalMaxX = x + 16 + (east ? 1 : 0);
        int verticalMinZ = z - (north ? 1 : 0);
        int verticalMaxZ = z + 16 + (south ? 1 : 0);
        if (!north) {
            overlays.add(createEdge(location, horizontalMinX, z, horizontalMaxX, z + 1));
        }
        if (!south) {
            overlays.add(createEdge(location, horizontalMinX, z + 15, horizontalMaxX, z + 16));
        }
        if (!west) {
            overlays.add(createEdge(location, x, verticalMinZ, x + 1, verticalMaxZ));
        }
        if (!east) {
            overlays.add(createEdge(location, x + 15, verticalMinZ, x + 16, verticalMaxZ));
        }
        return overlays;
    }

    private static PolygonOverlay createEdge(ClaimsLocation location, int minX, int minZ, int maxX, int maxZ) {
        ShapeProperties edge = new ShapeProperties().setFillColor(ClaimsRenderStep.LOADED_BORDER.rgb())
                .setFillOpacity(230F / 255F).setStrokeOpacity(0F);
        return createOverlay(
                location,
                edge,
                PolygonHelper.createBlockRect(new BlockPos(minX, 64, minZ), new BlockPos(maxX, 64, maxZ)),
                101,
                Context.UI.Minimap);
    }

    private static PolygonOverlay createOverlay(ClaimsLocation location, ShapeProperties shape, MapPolygon polygon,
            int displayOrder, Context.UI... activeUIs) {
        PolygonOverlay overlay = new PolygonOverlay(ServerUtilities.MOD_ID, location.getDimensionId(), shape, polygon);
        overlay.setOverlayGroupName(ClaimsLocation.class.getName()).setActiveUIs(activeUIs)
                .setDisplayOrder(displayOrder);
        return overlay;
    }
}
