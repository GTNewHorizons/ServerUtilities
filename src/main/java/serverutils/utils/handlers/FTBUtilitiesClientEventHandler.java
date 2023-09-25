package serverutils.utils.handlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.feed_the_beast.ftblib.events.client.CustomClickEvent;
import com.feed_the_beast.ftblib.lib.EnumTeamColor;
import com.feed_the_beast.ftblib.lib.client.ClientUtils;
import com.feed_the_beast.ftblib.lib.gui.misc.ChunkSelectorMap;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftblib.lib.math.Ticks;
import com.feed_the_beast.ftblib.lib.util.StringUtils;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesConfig;
import serverutils.utils.client.ServerUtilitiesClient;
import serverutils.utils.client.ServerUtilitiesClientConfig;
import serverutils.utils.events.chunks.UpdateClientDataEvent;
import serverutils.utils.gui.ClientClaimedChunks;
import serverutils.utils.gui.GuiClaimedChunks;
import serverutils.utils.net.MessageClaimedChunksUpdate;
import serverutils.utils.net.MessageEditNBTRequest;
import serverutils.utils.net.MessageLeaderboardList;
import serverutils.utils.net.MessageRequestBadge;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/**
 * @author LatvianModder
 */
public class FTBUtilitiesClientEventHandler {

    public static final FTBUtilitiesClientEventHandler INST = new FTBUtilitiesClientEventHandler();
    private static final Map<UUID, Icon> BADGE_CACHE = new HashMap<>();
    public static long shutdownTime = 0L;
    public static int currentPlaytime = 0;

    public static void readSyncData(NBTTagCompound nbt) {
        shutdownTime = System.currentTimeMillis() + nbt.getLong("ShutdownTime");
    }

    public static Icon getBadge(UUID id) {
        Icon tex = BADGE_CACHE.get(id);

        if (tex == null) {
            tex = Icon.EMPTY;
            BADGE_CACHE.put(id, tex);
            new MessageRequestBadge(id).sendToServer();
        }

        return tex;
    }

    public static void setBadge(UUID id, String url) {
        BADGE_CACHE.put(id, Icon.getIcon(url));
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        BADGE_CACHE.clear();
        shutdownTime = 0L;
    }

    @SubscribeEvent
    public void onDebugInfoEvent(RenderGameOverlayEvent.Text event) {
        if (Minecraft.getMinecraft().gameSettings.showDebugInfo) {
            return;
        }

        if (shutdownTime > 0L && ServerUtilitiesClientConfig.general.show_shutdown_timer) {
            long timeLeft = Math.max(0L, shutdownTime - System.currentTimeMillis());

            if (timeLeft > 0L && timeLeft <= ServerUtilitiesClientConfig.general.getShowShutdownTimer()) {
                event.left.add(
                        EnumChatFormatting.DARK_RED
                                + I18n.format("ftbutilities.lang.timer.shutdown", StringUtils.getTimeString(timeLeft)));
            }
        }

        if (ServerUtilitiesConfig.world.show_playtime) {
            event.left.add(
                    StatList.minutesPlayedStat.func_150951_e().getUnformattedText() + ": "
                            + Ticks.get(
                                    Minecraft.getMinecraft().thePlayer.getStatFileWriter()
                                            .writeStat(StatList.minutesPlayedStat))
                                    .toTimeString());
        }
    }

    @SubscribeEvent
    public void onKeyEvent(InputEvent.KeyInputEvent event) {
        if (ServerUtilitiesClient.KEY_NBT.isPressed()) {
            MessageEditNBTRequest.editNBT();
        }

        if (ServerUtilitiesClient.KEY_TRASH.isPressed()) {
            ClientUtils.execClientCommand("/trash_can");
        }
    }

    @SubscribeEvent
    public void onCustomClick(CustomClickEvent event) {
        if (event.getID().getResourceDomain().equals(ServerUtilities.MOD_ID)) {
            switch (event.getID().getResourcePath()) {
                case "toggle_gamemode":
                    ClientUtils.execClientCommand(
                            "/gamemode " + (Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode ? "survival"
                                    : "creative"));
                    break;
                case "daytime":
                    long addDay = (24000L - (Minecraft.getMinecraft().theWorld.getWorldTime() % 24000L)
                            + ServerUtilitiesClientConfig.general.button_daytime) % 24000L;

                    if (addDay != 0L) {
                        ClientUtils.execClientCommand("/time add " + addDay);
                    }

                    break;
                case "nighttime":
                    long addNight = (24000L - (Minecraft.getMinecraft().theWorld.getWorldTime() % 24000L)
                            + ServerUtilitiesClientConfig.general.button_nighttime) % 24000L;

                    if (addNight != 0L) {
                        ClientUtils.execClientCommand("/time add " + addNight);
                    }

                    break;
                case "claims_gui":
                    GuiClaimedChunks.instance = new GuiClaimedChunks();
                    GuiClaimedChunks.instance.openGui();
                    break;
                case "leaderboards_gui":
                    new MessageLeaderboardList().sendToServer();
                    break;
            }

            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onClientWorldTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();

        if (event.phase == TickEvent.Phase.START && mc.theWorld != null
                && mc.theWorld.provider.dimensionId == ServerUtilitiesConfig.world.spawn_dimension) {
            if (ServerUtilitiesConfig.world.forced_spawn_dimension_time != -1) {
                mc.theWorld.setWorldTime(ServerUtilitiesConfig.world.forced_spawn_dimension_time);
            }

            if (ServerUtilitiesConfig.world.forced_spawn_dimension_weather != -1) {
                mc.theWorld.getWorldInfo().setRaining(ServerUtilitiesConfig.world.forced_spawn_dimension_weather >= 1);
                mc.theWorld.getWorldInfo().setThundering(ServerUtilitiesConfig.world.forced_spawn_dimension_weather >= 2);
            }
        }
    }

    @SubscribeEvent
    public void onChunkDataUpdate(UpdateClientDataEvent event) {
        MessageClaimedChunksUpdate m = event.getMessage();
        GuiClaimedChunks.claimedChunks = m.claimedChunks;
        GuiClaimedChunks.loadedChunks = m.loadedChunks;
        GuiClaimedChunks.maxClaimedChunks = m.maxClaimedChunks;
        GuiClaimedChunks.maxLoadedChunks = m.maxLoadedChunks;
        Arrays.fill(GuiClaimedChunks.chunkData, null);

        for (ClientClaimedChunks.Team team : m.teams.values()) {
            for (Map.Entry<Integer, ClientClaimedChunks.ChunkData> entry : team.chunks.entrySet()) {
                int x = entry.getKey() % ChunkSelectorMap.TILES_GUI;
                int z = entry.getKey() / ChunkSelectorMap.TILES_GUI;
                GuiClaimedChunks.chunkData[x + z * ChunkSelectorMap.TILES_GUI] = entry.getValue();
            }
        }

        GuiClaimedChunks.AREA.reset();
        EnumTeamColor prevCol = null;
        ClientClaimedChunks.ChunkData data;

        for (int i = 0; i < GuiClaimedChunks.chunkData.length; i++) {
            data = GuiClaimedChunks.chunkData[i];

            if (data == null) {
                continue;
            }

            if (prevCol != data.team.color) {
                prevCol = data.team.color;
                GuiClaimedChunks.AREA.color.set(data.team.color.getColor(), 150);
            }

            GuiClaimedChunks.AREA.rect(
                    (i % ChunkSelectorMap.TILES_GUI) * GuiClaimedChunks.TILE_SIZE,
                    (i / ChunkSelectorMap.TILES_GUI) * GuiClaimedChunks.TILE_SIZE,
                    GuiClaimedChunks.TILE_SIZE,
                    GuiClaimedChunks.TILE_SIZE);
        }

        boolean borderU, borderD, borderL, borderR;

        for (int i = 0; i < GuiClaimedChunks.chunkData.length; i++) {
            data = GuiClaimedChunks.chunkData[i];

            if (data == null) {
                continue;
            }

            int x = i % ChunkSelectorMap.TILES_GUI;
            int dx = x * GuiClaimedChunks.TILE_SIZE;
            int y = i / ChunkSelectorMap.TILES_GUI;
            int dy = y * GuiClaimedChunks.TILE_SIZE;

            borderU = y > 0 && GuiClaimedChunks.hasBorder(data, GuiClaimedChunks.getAt(x, y - 1));
            borderD = y < (ChunkSelectorMap.TILES_GUI - 1)
                    && GuiClaimedChunks.hasBorder(data, GuiClaimedChunks.getAt(x, y + 1));
            borderL = x > 0 && GuiClaimedChunks.hasBorder(data, GuiClaimedChunks.getAt(x - 1, y));
            borderR = x < (ChunkSelectorMap.TILES_GUI - 1)
                    && GuiClaimedChunks.hasBorder(data, GuiClaimedChunks.getAt(x + 1, y));

            if (data.isLoaded()) {
                GuiClaimedChunks.AREA.color.set(255, 80, 80, 230);
            } else {
                GuiClaimedChunks.AREA.color.set(80, 80, 80, 230);
            }

            if (borderU) {
                GuiClaimedChunks.AREA.rect(dx, dy, GuiClaimedChunks.TILE_SIZE, 1);
            }

            if (borderD) {
                GuiClaimedChunks.AREA.rect(dx, dy + GuiClaimedChunks.TILE_SIZE - 1, GuiClaimedChunks.TILE_SIZE, 1);
            }

            if (borderL) {
                GuiClaimedChunks.AREA.rect(dx, dy, 1, GuiClaimedChunks.TILE_SIZE);
            }

            if (borderR) {
                GuiClaimedChunks.AREA.rect(dx + GuiClaimedChunks.TILE_SIZE - 1, dy, 1, GuiClaimedChunks.TILE_SIZE);
            }
        }
    }
}
