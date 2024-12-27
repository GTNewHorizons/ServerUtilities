package serverutils.client;

import static serverutils.client.ServerUtilitiesClient.CLIENT_FOLDER;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesNotifications;
import serverutils.lib.EnumMessageLocation;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.ConfigString;
import serverutils.lib.config.ConfigValueInstance;
import serverutils.lib.util.JsonUtils;
import serverutils.lib.util.text_components.Notification;

@EventBusSubscriber(side = Side.CLIENT)
public class NotificationHandler {

    private static final Deque<Notification> NOTIFICATIONS = new ArrayDeque<>();
    private static final Map<String, IChatComponent> lastMessages = new HashMap<>();
    private static ConfigGroup notificationConfig;
    private static NotificationWidget currentNotification;

    public static void onNotify(IChatComponent component) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(component instanceof Notification notification)) {
            mc.thePlayer.addChatMessage(component);
            return;
        }

        ResourceLocation id = notification.getId();
        ServerUtilitiesNotifications notificationType = ServerUtilitiesNotifications.getFromId(id);
        EnumMessageLocation location = (notificationType == null || notification.isImportant())
                ? EnumMessageLocation.ACTION_BAR
                : notificationType.getLocation();

        if (location == EnumMessageLocation.OFF) return;

        lastMessages.put(id.getResourcePath(), notification);
        if (location == EnumMessageLocation.CHAT) {
            mc.thePlayer.addChatMessage(component);
            return;
        }

        if (notification.isVanilla()) {
            mc.ingameGUI.func_110326_a(component.getFormattedText(), false);
            return;
        }

        NOTIFICATIONS.remove(notification);
        if (currentNotification != null && currentNotification.id.equals(id)) {
            currentNotification = null;
        }

        NOTIFICATIONS.add(notification);
    }

    public static void updateDescription(ConfigValueInstance inst) {
        IChatComponent notification = lastMessages.get(inst.getId());
        if (notification == null) return;
        IChatComponent info = inst.getInfo();
        if (info == null) return;
        info.getSiblings().clear();
        info.appendText("\n" + StatCollector.translateToLocal("serverutilities.notifications.last_received") + "\n")
                .appendSibling(notification);
    }

    static void loadNotifications() {
        JsonElement file = JsonUtils.fromJson(new File(CLIENT_FOLDER + "notifications.json"));
        if (JsonUtils.isNull(file)) return;

        ConfigGroup group = getNotificationConfig();
        for (Map.Entry<String, JsonElement> entry : file.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;

            JsonObject obj = entry.getValue().getAsJsonObject();
            group.getValue(entry.getKey())
                    .setValueFromString(null, obj.getAsJsonPrimitive("location").getAsString().toLowerCase(), false);
            if (obj.has("lastReceived")) {
                IChatComponent last = JsonUtils.deserializeTextComponent(obj.getAsJsonObject("lastReceived"));
                lastMessages.put(entry.getKey(), last);
            }
        }
    }

    public static void saveConfig(ConfigGroup group, ICommandSender sender) {
        JsonObject json = new JsonObject();
        for (ServerUtilitiesNotifications notification : ServerUtilitiesNotifications.VALUES) {
            JsonObject obj = new JsonObject();
            obj.add("location", new JsonPrimitive(notification.getLocation().name()));
            IChatComponent last = lastMessages.get(notification.getId());
            if (last != null) {
                obj.add("lastReceived", JsonUtils.serializeTextComponent(last));
            }

            json.add(notification.getId(), obj);
        }

        JsonUtils.toJsonSafe(new File(CLIENT_FOLDER + "notifications.json"), json);
    }

    public static ConfigGroup getNotificationConfig() {
        if (notificationConfig != null) return notificationConfig;

        notificationConfig = ConfigGroup.newGroup("notifications")
                .setDisplayName(ServerUtilities.lang("serverutilities.notifications.config"));
        for (ServerUtilitiesNotifications notification : ServerUtilitiesNotifications.values()) {
            notificationConfig
                    .addEnum(
                            notification.getId(),
                            notification::getLocation,
                            notification::setLocation,
                            EnumMessageLocation.NAME_MAP)
                    .setDefaultValue(new ConfigString(notification.getLocation().name()))
                    .setDisplayName(
                            new ChatComponentTranslation("serverutilities.notifications." + notification.getId()))
                    .setInfo(new ChatComponentText(notification.getDesc()));
        }

        return notificationConfig;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (Minecraft.getMinecraft().theWorld == null) {
                currentNotification = null;
                NOTIFICATIONS.clear();
            }

            if (currentNotification != null) {
                if (currentNotification.tick()) {
                    currentNotification = null;
                }
            }

            if (currentNotification == null && !NOTIFICATIONS.isEmpty()) {
                currentNotification = new NotificationWidget(NOTIFICATIONS.pop());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onGameOverlayRender(RenderGameOverlayEvent.Text event) {
        if (currentNotification != null && !currentNotification.isImportant()) {
            currentNotification.render(event.resolution, event.partialTicks);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.enableTexture2D();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (currentNotification != null && currentNotification.isImportant()) {
            Minecraft mc = Minecraft.getMinecraft();
            currentNotification
                    .render(new ScaledResolution(mc, mc.displayWidth, mc.displayHeight), event.renderTickTime);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.enableTexture2D();
        }
    }

    private static class NotificationWidget {

        private static final FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        public final Notification notification;
        public final ResourceLocation id;
        public final List<String> text;
        public int width, height;
        public final long timer;
        private long tick, endTick;

        public NotificationWidget(Notification n) {
            notification = n;
            id = n.getId();
            width = 0;
            text = new ArrayList<>();
            timer = n.getTimer().ticks();
            tick = endTick = -1L;

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
            int offy = (text.size() * 11) / 2;

            for (int i = 0; i < text.size(); i++) {
                String string = text.get(i);
                font.drawStringWithShadow(
                        string,
                        width - font.getStringWidth(string) / 2,
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
                endTick = tick + timer;
            }
            return tick >= endTick || Math.min(255F, (endTick - tick) * 255F / 20F) <= 2F;
        }

        private boolean isImportant() {
            return notification.isImportant();
        }
    }
}
