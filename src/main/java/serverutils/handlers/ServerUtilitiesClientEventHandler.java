package serverutils.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
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
import serverutils.integration.vp.VPIntegration;
import serverutils.lib.OtherMods;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.gui.Widget;
import serverutils.lib.icon.IconRenderer;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.InvUtils;
import serverutils.lib.util.NBTUtils;
import serverutils.lib.util.SidedUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.text_components.Notification;
import serverutils.net.MessageAdminPanelGui;
import serverutils.net.MessageClaimedChunksUpdate;
import serverutils.net.MessageEditNBTRequest;
import serverutils.net.MessageLeaderboardList;
import serverutils.net.MessageMyTeamGui;

public class ServerUtilitiesClientEventHandler {

    public static final ServerUtilitiesClientEventHandler INST = new ServerUtilitiesClientEventHandler();
    private static Temp currentNotification;
    public static boolean shouldRenderIcons = false;
    public static long shutdownTime = 0L;

    public static void readSyncData(NBTTagCompound nbt) {
        shutdownTime = System.currentTimeMillis() + nbt.getLong("ShutdownTime");
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        shutdownTime = 0L;
        SidedUtils.SERVER_MODS.clear();
        if (OtherMods.isVPLoaded()) {
            VPIntegration.CLAIMS.clear();
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
        if (OtherMods.isVPLoaded()) {
            VPIntegration.onChunkDataUpdate(message);
        }
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

    public void onNotify(IChatComponent component) {
        boolean importantNotification = component instanceof Notification noti && noti.isImportant();

        if (ServerUtilitiesClientConfig.notifications.disabled() && !importantNotification) {
            return;
        }

        if (ServerUtilitiesClientConfig.notifications.chat() && !importantNotification) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(component);
        } else if (component instanceof Notification notification) {
            ResourceLocation id = notification.getId();

            if (notification.isVanilla()) {
                Minecraft.getMinecraft().ingameGUI.func_110326_a(component.getFormattedText(), false);
                return;
            }

            Temp.MAP.remove(id);
            if (currentNotification != null && currentNotification.widget.id.equals(id)) {
                currentNotification = null;
            }
            Temp.MAP.put(id, notification);
        }
    }

    @SubscribeEvent
    public void onClientChatEvent(ClientChatReceivedEvent event) {
        IChatComponent component = event.message;
        if (component instanceof Notification notification) {
            onNotify(notification);
            event.message = null;
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
        if (event.phase == TickEvent.Phase.START) {
            if (Minecraft.getMinecraft().theWorld == null) {
                currentNotification = null;
                Temp.MAP.clear();
            }

            if (currentNotification != null) {
                if (currentNotification.tick()) {
                    currentNotification = null;
                }
            }

            if (currentNotification == null && !Temp.MAP.isEmpty()) {
                currentNotification = new Temp(Temp.MAP.values().iterator().next());
                Temp.MAP.remove(currentNotification.widget.id);
            }
        } else if (event.phase == TickEvent.Phase.END) {
            if (!ClientUtils.RUN_LATER.isEmpty()) {
                for (Runnable runnable : new ArrayList<>(ClientUtils.RUN_LATER)) {
                    runnable.run();
                }
                ClientUtils.RUN_LATER.clear();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onGameOverlayRender(RenderGameOverlayEvent.Text event) {
        if (currentNotification != null && !currentNotification.isImportant()) {
            currentNotification.render(event.resolution, event.partialTicks);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.enableTexture2D();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onRenderTick(TickEvent.RenderTickEvent event) {

        if (event.phase == TickEvent.Phase.START) {
            if (shouldRenderIcons) {
                IconRenderer.render();
            }
        } else if (currentNotification != null && currentNotification.isImportant()) {
            Minecraft mc = Minecraft.getMinecraft();
            currentNotification
                    .render(new ScaledResolution(mc, mc.displayWidth, mc.displayHeight), event.renderTickTime);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.enableTexture2D();
        }
    }

    public static class NotificationWidget {

        public final IChatComponent notification;
        public final ResourceLocation id;
        public final List<String> text;
        public int width, height;
        public final FontRenderer font;
        public final long timer;

        public NotificationWidget(IChatComponent n, FontRenderer f) {
            notification = n;
            id = n instanceof Notification ? ((Notification) n).getId() : Notification.VANILLA_STATUS;
            width = 0;
            font = f;
            text = new ArrayList<>();
            timer = n instanceof Notification ? ((Notification) n).getTimer().ticks() : 60L;

            String s0;

            try {
                s0 = notification.getFormattedText();
            } catch (Exception ex) {
                s0 = EnumChatFormatting.RED + ex.toString();
            }

            Minecraft mc = Minecraft.getMinecraft();
            for (String s : font.listFormattedStringToWidth(
                    s0,
                    new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaledWidth())) {
                for (String line : s.split("\n")) {
                    if (!line.isEmpty()) {
                        line = line.trim();
                        text.add(line);
                        width = Math.max(width, font.getStringWidth(line));
                    }
                }
            }

            width += 4;
            height = text.size() * 11;

            if (text.isEmpty()) {
                width = 20;
                height = 20;
            }
        }
    }

    private static class Temp {

        private static final LinkedHashMap<ResourceLocation, IChatComponent> MAP = new LinkedHashMap<>();
        private final NotificationWidget widget;
        private long tick, endTick;

        private Temp(IChatComponent n) {
            widget = new NotificationWidget(n, Minecraft.getMinecraft().fontRenderer);
            tick = endTick = -1L;
        }

        public void render(ScaledResolution screen, float partialTicks) {
            if (tick == -1L || tick >= endTick) {
                return;
            }

            int alpha = (int) Math.min(255F, (endTick - tick - partialTicks) * 255F / 20F);

            if (alpha <= 2) {
                return;
            }

            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.color(1F, 1F, 1F, 1F);

            int width = screen.getScaledWidth() / 2;
            int height = screen.getScaledHeight() - 67;
            int offy = (widget.text.size() * 11) / 2;

            for (int i = 0; i < widget.text.size(); i++) {
                String string = widget.text.get(i);
                widget.font.drawStringWithShadow(
                        string,
                        width - widget.font.getStringWidth(string) / 2,
                        height - offy + i * 11,
                        0xFFFFFF | (alpha << 24));
            }

            GlStateManager.depthMask(true);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
            GlStateManager.enableDepth();
        }

        private boolean tick() {
            tick = Minecraft.getMinecraft().theWorld.getTotalWorldTime();

            if (endTick == -1L) {
                endTick = tick + widget.timer;
            }
            return tick >= endTick || Math.min(255F, (endTick - tick) * 255F / 20F) <= 2F;
        }

        private boolean isImportant() {
            return widget.notification instanceof Notification notification && notification.isImportant();
        }
    }
}
