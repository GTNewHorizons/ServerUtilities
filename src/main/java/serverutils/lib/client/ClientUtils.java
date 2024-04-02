package serverutils.lib.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.ClientCommandHandler;

import codechicken.nei.GuiExtendedCreativeInv;
import serverutils.ServerUtilities;
import serverutils.client.EnumSidebarLocation;
import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.client.gui.SidebarButtonManager;
import serverutils.lib.OtherMods;
import serverutils.lib.icon.PlayerHeadIcon;

public class ClientUtils {

    public static final BooleanSupplier IS_CLIENT_OP = ClientUtils::isClientOpped;
    public static final List<Runnable> RUN_LATER = new ArrayList<>();

    // assume opped until told otherwise
    public static boolean is_op = true;
    private static float lastBrightnessX, lastBrightnessY;

    public static PlayerHeadIcon localPlayerHead;

    public static int getDim() {
        return Minecraft.getMinecraft().theWorld != null ? Minecraft.getMinecraft().theWorld.provider.dimensionId : 0;
    }

    public static void spawnParticle(EntityFX particle) {
        Minecraft.getMinecraft().effectRenderer.addEffect(particle);
    }

    public static void pushBrightness(int u, int t) {
        lastBrightnessX = OpenGlHelper.lastBrightnessX;
        lastBrightnessY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, u, t);
    }

    public static void pushMaxBrightness() {
        pushBrightness(240, 240);
    }

    public static void popBrightness() {
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
    }

    public static void execClientCommand(String command, boolean printChat) {
        if (printChat) {
            Minecraft.getMinecraft().ingameGUI.getChatGUI().addToSentMessages(command);
        }

        if (ClientCommandHandler.instance.executeCommand(Minecraft.getMinecraft().thePlayer, command) == 0) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(command);
        }
    }

    public static void execClientCommand(String command) {
        execClientCommand(command, false);
    }

    public static void runLater(final Runnable runnable) {
        RUN_LATER.add(runnable);
    }

    public static boolean isClientOpped() {
        return is_op;
    }

    public static boolean areButtonsVisible(@Nullable GuiScreen gui) {
        return ServerUtilitiesClientConfig.sidebar_buttons != EnumSidebarLocation.DISABLED
                && (gui instanceof InventoryEffectRenderer || isCreativePlusGui(gui))
                && !SidebarButtonManager.INSTANCE.groups.isEmpty();
    }

    public static boolean isCreativePlusGui(GuiScreen gui) {
        if (OtherMods.isNEILoaded()) {
            return gui instanceof GuiExtendedCreativeInv;
        }
        return false;
    }

    public static String getDisabledTip() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isSingleplayer()) {
            return StatCollector.translateToLocal(ServerUtilities.MOD_ID + ".disabled.config");
        } else {
            return StatCollector.translateToLocal(ServerUtilities.MOD_ID + ".disabled.server");
        }
    }
}
