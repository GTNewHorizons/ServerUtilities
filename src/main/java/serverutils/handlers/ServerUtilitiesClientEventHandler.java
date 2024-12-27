package serverutils.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.client.ServerUtilitiesClient;
import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.client.gui.GuiClaimedChunks;
import serverutils.client.gui.GuiClientConfig;
import serverutils.client.gui.GuiSidebar;
import serverutils.events.chunks.UpdateClientDataEvent;
import serverutils.events.client.CustomClickEvent;
import serverutils.integration.navigator.NavigatorIntegration;
import serverutils.lib.OtherMods;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.gui.Widget;
import serverutils.lib.icon.IconRenderer;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.InvUtils;
import serverutils.lib.util.NBTUtils;
import serverutils.lib.util.SidedUtils;
import serverutils.lib.util.StringUtils;
import serverutils.net.MessageAdminPanelGui;
import serverutils.net.MessageClaimedChunksUpdate;
import serverutils.net.MessageEditNBTRequest;
import serverutils.net.MessageLeaderboardList;
import serverutils.net.MessageMyTeamGui;

public class ServerUtilitiesClientEventHandler {

    public static final ServerUtilitiesClientEventHandler INST = new ServerUtilitiesClientEventHandler();
    public static boolean shouldRenderIcons = false;
    public static long shutdownTime = 0L;

    private static final List<String> sidebarButtonTooltip = new ArrayList<>();

    public static void readSyncData(NBTTagCompound nbt) {
        shutdownTime = System.currentTimeMillis() + nbt.getLong("ShutdownTime");
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        shutdownTime = 0L;
        SidedUtils.SERVER_MODS.clear();
        if (OtherMods.isNavigatorLoaded()) {
            NavigatorIntegration.CLAIMS.clear();
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
                mc.theWorld.getWorldInfo()
                        .setThundering(ServerUtilitiesConfig.world.forced_spawn_dimension_weather >= 2);
            }
        }
    }

    @SubscribeEvent
    public void onChunkDataUpdate(UpdateClientDataEvent event) {
        MessageClaimedChunksUpdate message = event.getMessage();
        GuiClaimedChunks.onChunkDataUpdate(message);
        if (OtherMods.isNavigatorLoaded()) {
            NavigatorIntegration.onChunkDataUpdate(message);
        }
    }

    @SubscribeEvent
    public void onDebugInfoEvent(RenderGameOverlayEvent.Text event) {

        if (Minecraft.getMinecraft().gameSettings.showDebugInfo) {
            return;
        }

        if (shutdownTime > 0L && ServerUtilitiesClientConfig.show_shutdown_timer) {
            long timeLeft = Math.max(0L, shutdownTime - System.currentTimeMillis());

            if (timeLeft > 0L && timeLeft <= ServerUtilitiesClientConfig.getShowShutdownTimer()) {
                event.left.add(
                        EnumChatFormatting.DARK_RED + I18n
                                .format("serverutilities.lang.timer.shutdown", StringUtils.getTimeString(timeLeft)));
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
                case "client_config_gui":
                    new GuiClientConfig().openGui();
                    break;
                case "my_team_gui":
                    new MessageMyTeamGui().sendToServer();
                    break;
                case "admin_panel_gui":
                    new MessageAdminPanelGui().sendToServer();
                    break;
                case "toggle_gamemode":
                    ClientUtils.execClientCommand(
                            "/gamemode " + (Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode ? "survival"
                                    : "creative"));
                    break;
                case "daytime":
                    long addDay = (24000L - (Minecraft.getMinecraft().theWorld.getWorldTime() % 24000L)
                            + ServerUtilitiesClientConfig.button_daytime) % 24000L;

                    if (addDay != 0L) {
                        ClientUtils.execClientCommand("/time add " + addDay);
                    }

                    break;
                case "nighttime":
                    long addNight = (24000L - (Minecraft.getMinecraft().theWorld.getWorldTime() % 24000L)
                            + ServerUtilitiesClientConfig.button_nighttime) % 24000L;

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
    public void onTooltip(ItemTooltipEvent event) {
        if (ServerUtilitiesClientConfig.item_ore_names) {
            Collection<String> ores = InvUtils.getOreNames(null, event.itemStack);

            if (!ores.isEmpty()) {
                event.toolTip.add(I18n.format("serverutilities_client.item_ore_names.item_tooltip"));

                for (String or : ores) {
                    event.toolTip.add("> " + or);
                }
            }
        }

        if (ServerUtilitiesClientConfig.item_nbt && GuiScreen.isShiftKeyDown()) {
            NBTTagCompound nbt = Widget.isAltKeyDown() ? event.itemStack.writeToNBT(new NBTTagCompound())
                    : event.itemStack.getTagCompound();

            if (nbt != null) {
                event.toolTip.add(NBTUtils.getColoredNBTString(nbt));
            }
        }
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void onGuiInit(final GuiScreenEvent.InitGuiEvent.Post event) {
        if (ClientUtils.areButtonsVisible(event.gui)) {
            event.buttonList.add(new GuiSidebar((GuiContainer) event.gui));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ClientUtils.RUN_LATER.isEmpty()) {
            for (Runnable runnable : new ArrayList<>(ClientUtils.RUN_LATER)) {
                runnable.run();
            }
            ClientUtils.RUN_LATER.clear();
        }
    }

    /**
     * Renders sidebar button tooltips outside of {@link GuiSidebar#drawButton} so that other screen elements don't draw
     * over it.
     */
    @SubscribeEvent
    public void onGuiScreenDraw(final GuiScreenEvent.DrawScreenEvent.Post event) {
        if (ClientUtils.areButtonsVisible(event.gui)) {
            event.gui.buttonList.forEach((GuiButton button) -> {
                if (button instanceof GuiSidebar sidebar) {
                    sidebarButtonTooltip.clear();
                    sidebar.addTooltip(sidebarButtonTooltip);
                    event.gui.func_146283_a(sidebarButtonTooltip, event.mouseX, event.mouseY);
                }
            });
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START && shouldRenderIcons) {
            IconRenderer.render();
        }
    }
}
