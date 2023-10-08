package serverutils.utils.integration;

// import serverutils.lib.lib.client.ClientUtils;
// import serverutils.lib.lib.gui.misc.ChunkSelectorMap;
// import serverutils.lib.lib.math.MathUtils;
// import serverutils.mod.ServerUtilities;
// import serverutils.utils.client.ServerUtilitiesClientConfig;
// import serverutils.utils.chunks.events.UpdateClientDataEvent;
// import serverutils.utils.gui.ClientClaimedChunks;
// import serverutils.utils.net.MessageClaimedChunksRequest;
// import serverutils.utils.net.MessageClaimedChunksUpdate;
// import journeymap.client.api.ClientPlugin;
// import journeymap.client.api.IClientAPI;
// import journeymap.client.api.IClientPlugin;
// import journeymap.client.api.display.DisplayType;
// import journeymap.client.api.display.PolygonOverlay;
// import journeymap.client.api.event.ClientEvent;
// import journeymap.client.api.model.MapPolygon;
// import journeymap.client.api.model.ShapeProperties;
// import journeymap.client.api.util.PolygonHelper;
// import net.minecraft.client.Minecraft;
// import net.minecraft.client.resources.I18n;
// import net.minecraft.util.math.ChunkPos;
// import net.minecraft.util.EnumChatFormatting;
// import net.minecraftforge.common.MinecraftForge;
// import net.minecraftforge.event.entity.EntityEvent;
// import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//
// import javax.annotation.Nullable;
// import java.util.EnumSet;
// import java.util.HashMap;
// import java.util.Map;

// @ClientPlugin
public class JourneyMapIntegration // implements IClientPlugin
{
    // private IClientAPI clientAPI;
    // private Map<ChunkPos, PolygonOverlay> polygons;
    // private ChunkPos lastPosition;
    //
    // @Override
    // public void initialize(IClientAPI api) {
    // if (!ServerUtilitiesClientConfig.general.journeymap_overlay) {
    // return;
    // }
    //
    // clientAPI = api;
    // polygons = new HashMap<>();
    // lastPosition = null;
    // api.subscribe(getModId(), EnumSet.of(ClientEvent.Type.DISPLAY_UPDATE, ClientEvent.Type.MAPPING_STOPPED));
    // MinecraftForge.EVENT_BUS.register(this);
    // }
    //
    // @Override
    // public String getModId() {
    // return ServerUtilities.MOD_ID;
    // }
    //
    // @Override
    // public void onEvent(ClientEvent event) {
    // switch (event.type) {
    // case DISPLAY_UPDATE:
    // if (Minecraft.getMinecraft().player != null) {
    // new MessageClaimedChunksRequest(Minecraft.getMinecraft().player).sendToServer();
    // }
    // break;
    // case MAPPING_STOPPED:
    // clearData();
    // break;
    // }
    // }
    //
    // private void clearData() {
    // if (!polygons.isEmpty()) {
    // polygons.clear();
    // clientAPI.removeAll(ServerUtilities.MOD_ID);
    // }
    // }
    //
    // private void chunkChanged(ChunkPos pos, @Nullable ClientClaimedChunks.ChunkData chunk, int dim) {
    // try {
    // PolygonOverlay p = polygons.get(pos);
    //
    // if (p != null) {
    // clientAPI.remove(p);
    //
    // if (chunk == null) {
    // polygons.remove(pos);
    // }
    // }
    //
    // if (chunk != null) {
    // MapPolygon poly = PolygonHelper.createChunkPolygon(pos.x, 0, pos.z);
    // p = new PolygonOverlay(ServerUtilities.MOD_ID, "claimed_" + pos.x + '_' + pos.z, dim,
    // (ShapeProperties) chunk.team.shapeProperties, poly);
    // p.setOverlayGroupName("claimed_chunks");
    // p.setTitle(chunk.team.nameComponent.getFormattedText() + '\n' + EnumChatFormatting.GREEN
    // + I18n.format("serverutilities.lang.chunks.claimed_area"));
    // polygons.put(pos, p);
    // clientAPI.show(p);
    // }
    // } catch (Exception ex) {
    // ex.printStackTrace();
    // }
    // }
    //
    // @SubscribeEvent
    // public void onEnteringChunk(EntityEvent.EnteringChunk event) {
    // if (event.getEntity() != Minecraft.getMinecraft().player) {
    // return;
    // }
    //
    // if (lastPosition == null
    // || MathUtils.dist(event.getNewChunkX(), event.getNewChunkZ(), lastPosition.x, lastPosition.z) >= 3D) {
    // lastPosition = new ChunkPos(event.getNewChunkX(), event.getNewChunkZ());
    // new MessageClaimedChunksRequest(Minecraft.getMinecraft().player).sendToServer();
    // }
    // }
    //
    // @SubscribeEvent
    // public void onDataReceived(UpdateClientDataEvent event) {
    // if (!polygons.isEmpty() && !clientAPI.playerAccepts(ServerUtilities.MOD_ID, DisplayType.Polygon)) {
    // clearData();
    // return;
    // }
    //
    // MessageClaimedChunksUpdate m = event.getMessage();
    // ClientClaimedChunks.ChunkData[] data = new ClientClaimedChunks.ChunkData[ChunkSelectorMap.TILES_GUI
    // * ChunkSelectorMap.TILES_GUI];
    //
    // for (ClientClaimedChunks.Team team : m.teams.values()) {
    // ShapeProperties shapeProperties = new ShapeProperties();
    // shapeProperties.setFillOpacity(0.2F);
    // shapeProperties.setStrokeOpacity(0F);
    // shapeProperties.setFillColor(team.color.getColor().rgba());
    // team.shapeProperties = shapeProperties;
    //
    // for (Map.Entry<Integer, ClientClaimedChunks.ChunkData> entry : team.chunks.entrySet()) {
    // int x = entry.getKey() % ChunkSelectorMap.TILES_GUI;
    // int z = entry.getKey() / ChunkSelectorMap.TILES_GUI;
    // data[x + z * ChunkSelectorMap.TILES_GUI] = entry.getValue();
    // }
    // }
    //
    // int dim = ClientUtils.getDim();
    //
    // for (int z = 0; z < ChunkSelectorMap.TILES_GUI; z++) {
    // for (int x = 0; x < ChunkSelectorMap.TILES_GUI; x++) {
    // chunkChanged(new ChunkPos(m.startX + x, m.startZ + z), data[x + z * ChunkSelectorMap.TILES_GUI], dim);
    // }
    // }
    // }
}
