package serverutils.handlers;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.renderer.InventoryEffectRenderer;
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

import org.lwjgl.opengl.GL11;

import codechicken.nei.GuiExtendedCreativeInv;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.client.EnumSidebarButtonPlacement;
import serverutils.client.ServerUtilitiesClient;
import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.client.gui.GuiClaimedChunks;
import serverutils.client.gui.GuiClientConfig;
import serverutils.client.gui.SidebarButton;
import serverutils.client.gui.SidebarButtonGroup;
import serverutils.client.gui.SidebarButtonManager;
import serverutils.events.chunks.UpdateClientDataEvent;
import serverutils.events.client.CustomClickEvent;
import serverutils.integration.vp.VPIntegration;
import serverutils.lib.OtherMods;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.gui.Widget;
import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.IconRenderer;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.InvUtils;
import serverutils.lib.util.NBTUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.text_components.Notification;
import serverutils.net.MessageAdminPanelGui;
import serverutils.net.MessageEditNBTRequest;
import serverutils.net.MessageLeaderboardList;
import serverutils.net.MessageMyTeamGui;
import serverutils.net.MessageRequestBadge;

public class ServerUtilitiesClientEventHandler {

    public static final ServerUtilitiesClientEventHandler INST = new ServerUtilitiesClientEventHandler();
    private static Temp currentNotification;
    public static Rectangle lastDrawnArea = new Rectangle();
    public static boolean shouldRenderIcons = false;
    private static final Map<UUID, Icon> BADGE_CACHE = new HashMap<>();
    public static long shutdownTime = 0L;

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
        GuiClaimedChunks.onChunkDataUpdate(event);
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

    @SubscribeEvent
    public void onGuiInit(final GuiScreenEvent.InitGuiEvent.Post event) {
        if (areButtonsVisible(event.gui)) {
            event.buttonList.add(new GuiButtonSidebarGroup((GuiContainer) event.gui));
        }
    }

    public static boolean areButtonsVisible(@Nullable GuiScreen gui) {
        return ServerUtilitiesClientConfig.sidebar_buttons != EnumSidebarButtonPlacement.DISABLED
                && (gui instanceof InventoryEffectRenderer || isCreativePlusGui(gui))
                && !SidebarButtonManager.INSTANCE.groups.isEmpty();
    }

    private static boolean isCreativePlusGui(GuiScreen gui) {
        if (OtherMods.isNEILoaded()) {
            return gui instanceof GuiExtendedCreativeInv;
        }
        return false;
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFrameStart(TickEvent.RenderTickEvent e) {}

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
            for (String s : (List<String>) font.listFormattedStringToWidth(
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
        private long tick, endTick;
        private NotificationWidget widget;

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
            GlStateManager.translate((int) (screen.getScaledWidth() / 2F), (int) (screen.getScaledHeight() - 67F), 0F);
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.color(1F, 1F, 1F, 1F);

            int offy = -(widget.text.size() * 11) / 2;

            for (int i = 0; i < widget.text.size(); i++) {
                String string = widget.text.get(i);
                widget.font.drawStringWithShadow(
                        string,
                        (int) (-widget.font.getStringWidth(string) / 2F),
                        offy + i * 11,
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
            return widget.notification instanceof Notification && ((Notification) widget.notification).isImportant();
        }
    }

    private static class GuiButtonSidebar {

        public final int buttonX, buttonY;
        public final SidebarButton button;
        public int x, y;

        public GuiButtonSidebar(int x, int y, SidebarButton b) {
            buttonX = x;
            buttonY = y;
            button = b;
        }
    }

    private static class GuiButtonSidebarGroup extends GuiButton {

        private final GuiContainer gui;
        public final List<GuiButtonSidebar> buttons;
        private GuiButtonSidebar mouseOver;

        public GuiButtonSidebarGroup(GuiContainer g) {
            super(495829, 0, 0, 0, 0, "");
            gui = g;
            buttons = new ArrayList<>();
        }

        @Override
        public void drawButton(Minecraft mc, int mx, int my) {
            buttons.clear();
            mouseOver = null;
            int rx = 0, ry = 0;
            boolean addedAny;
            boolean top = ServerUtilitiesClientConfig.sidebar_buttons.top();
            boolean above = ServerUtilitiesClientConfig.sidebar_buttons.above();
            boolean vertical = ServerUtilitiesClientConfig.sidebar_buttons.vertical();

            for (SidebarButtonGroup group : SidebarButtonManager.INSTANCE.groups) {
                if (above && !isCreativePlusGui(gui)) {
                    // If drawn above they are drawn in a horizontal line of 7 buttons
                    // roughly the same length as a potion label.
                    for (SidebarButton button : group.getButtons()) {
                        if (button.isActuallyVisible()) {
                            buttons.add(new GuiButtonSidebar(rx, ry, button));
                            ry++;
                            if (ry >= 7) {
                                ry = 0;
                                rx--;
                            }
                        }
                    }
                } else if (vertical) {
                    for (SidebarButton button : group.getButtons()) {
                        if (button.isActuallyVisible()) {
                            buttons.add(new GuiButtonSidebar(rx, ry, button));
                            rx++;
                            if (rx >= 9) {
                                rx = 0;
                                ry++;
                            }
                        }
                    }
                } else {
                    rx = 0;
                    addedAny = false;
                    for (SidebarButton button : group.getButtons()) {
                        if (button.isActuallyVisible()) {
                            buttons.add(new GuiButtonSidebar(rx, ry, button));
                            rx++;
                            addedAny = true;
                        }
                    }

                    if (addedAny) {
                        ry++;
                    }
                }
            }

            int guiLeft = gui.guiLeft;
            int guiTop = gui.guiTop;

            if (top) {
                for (GuiButtonSidebar button : buttons) {
                    button.x = 1 + button.buttonX * 17;
                    button.y = 1 + button.buttonY * 17;
                }
            } else {
                int offsetX = 18;
                int offsetY = 8;

                if (gui instanceof GuiContainerCreative) {
                    offsetY = 6;
                }

                if (above) {
                    offsetX = 22;
                    offsetY = -18;
                }

                if (isCreativePlusGui(gui)) {
                    offsetY = 22;
                    offsetX = 41;
                }

                for (GuiButtonSidebar button : buttons) {
                    button.x = guiLeft - offsetX - button.buttonY * 17;
                    button.y = guiTop + offsetY + button.buttonX * 17;
                }
            }

            int x = Integer.MAX_VALUE;
            int y = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (GuiButtonSidebar b : buttons) {
                if (b.x >= 0 && b.y >= 0) {
                    x = Math.min(x, b.x);
                    y = Math.min(y, b.y);
                    maxX = Math.max(maxX, b.x + 16);
                    maxY = Math.max(maxY, b.y + 16);
                }

                if (mx >= b.x && my >= b.y && mx < b.x + 16 && my < b.y + 16) {
                    mouseOver = b;
                }
            }

            x -= 2;
            y -= 2;
            maxX += 2;
            maxY += 2;

            width = maxX - x;
            height = maxY - y;
            zLevel = 0F;

            xPosition = x;
            yPosition = y;

            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 500);

            FontRenderer font = mc.fontRenderer;

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1F, 1F, 1F, 1F);

            for (GuiButtonSidebar b : buttons) {
                b.button.getIcon().draw(b.x, b.y, 16, 16);

                if (b == mouseOver) {
                    Color4I.WHITE.withAlpha(33).draw(b.x, b.y, 16, 16);
                }

                if (b.button.getCustomTextHandler() != null) {
                    String text = b.button.getCustomTextHandler().get();

                    if (!text.isEmpty()) {
                        int nw = font.getStringWidth(text);
                        int width = 16;
                        Color4I.LIGHT_RED.draw(b.x + width - nw, b.y - 1, nw + 1, 9);
                        font.drawString(text, b.x + width - nw + 1, b.y, 0xFFFFFFFF);
                        GlStateManager.color(1F, 1F, 1F, 1F);
                    }
                }
            }

            if (mouseOver != null) {
                int mx1 = mx + 10;
                int my1 = Math.max(3, my - 9);

                List<String> list = new ArrayList<>();
                list.add(I18n.format(mouseOver.button.getLangKey()));

                if (mouseOver.button.getTooltipHandler() != null) {
                    mouseOver.button.getTooltipHandler().accept(list);
                }

                int tw = 0;

                for (String s : list) {
                    tw = Math.max(tw, font.getStringWidth(s));
                }

                GlStateManager.pushMatrix();
                GlStateManager.enableDepth();
                GlStateManager.translate(0, 0, 500);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                Color4I.DARK_GRAY.draw(mx1 - 3, my1 - 2, tw + 6, 2 + list.size() * 10);

                for (int i = 0; i < list.size(); i++) {
                    font.drawString(list.get(i), mx1, my1 + i * 10, 0xFFFFFFFF);
                }
                GlStateManager.color(1F, 1F, 1F, 1F);
                GlStateManager.disableDepth();
                GlStateManager.popMatrix();
            }

            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.popMatrix();
            zLevel = 0F;

            lastDrawnArea = new Rectangle(xPosition, yPosition, width, height);
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mx, int my) {
            if (super.mousePressed(mc, mx, my)) {
                if (mouseOver != null) {
                    mouseOver.button.onClicked(GuiScreen.isShiftKeyDown());
                }
                return true;
            }
            return false;
        }
    }
}
