package serverutils.lib.mod.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.client.GlStateManager;
import serverutils.lib.api.client.LMFrustrumUtils;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.gui.callback.ClientTickCallback;
import serverutils.lib.api.notification.ClientNotifications;

@SideOnly(Side.CLIENT)
public class ServerUtilitiesLibraryRenderHandler {

    public static final ServerUtilitiesLibraryRenderHandler instance = new ServerUtilitiesLibraryRenderHandler();
    public static final List<ClientTickCallback> callbacks = new ArrayList<>();

    @SubscribeEvent(priority = EventPriority.LOW)
    public void renderTick(TickEvent.RenderTickEvent e) {
        GlStateManager.pushAttrib();
        GlStateManager.pushMatrix();

        if (e.phase == TickEvent.Phase.START) {
            ScaledResolution sr = new ScaledResolution(
                    ServerUtilitiesLibraryClient.mc,
                    ServerUtilitiesLibraryClient.mc.displayWidth,
                    ServerUtilitiesLibraryClient.mc.displayHeight);
            ServerUtilitiesLibraryClient.displayW = sr.getScaledWidth();
            ServerUtilitiesLibraryClient.displayH = sr.getScaledHeight();
        }

        if (e.phase == TickEvent.Phase.END && ServerUtilitiesLibraryClient.isIngame()) ClientNotifications.renderTemp();

        GlStateManager.popMatrix();
        GlStateManager.popAttrib();
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && !callbacks.isEmpty()) {
            for (int i = 0; i < callbacks.size(); i++) callbacks.get(i).onCallback();
            callbacks.clear();
        }
    }

    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent e) {
        LMFrustrumUtils.update();
        if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
            ServerUtilitiesLib.serverUtilitiesIntegration.renderWorld(e.partialTicks);
    }
}
