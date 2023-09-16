package ftb.lib.mod.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import cpw.mods.fml.common.eventhandler.*;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.FTBLib;
import ftb.lib.api.client.FTBLibClient;
import ftb.lib.api.client.GlStateManager;
import ftb.lib.api.client.LMFrustrumUtils;
import ftb.lib.api.gui.callback.ClientTickCallback;
import ftb.lib.api.notification.ClientNotifications;

@SideOnly(Side.CLIENT)
public class FTBLibRenderHandler {

    public static final FTBLibRenderHandler instance = new FTBLibRenderHandler();
    public static final List<ClientTickCallback> callbacks = new ArrayList<>();

    @SubscribeEvent(priority = EventPriority.LOW)
    public void renderTick(TickEvent.RenderTickEvent e) {
        GlStateManager.pushAttrib();
        GlStateManager.pushMatrix();

        if (e.phase == TickEvent.Phase.START) {
            ScaledResolution sr = new ScaledResolution(
                    FTBLibClient.mc,
                    FTBLibClient.mc.displayWidth,
                    FTBLibClient.mc.displayHeight);
            FTBLibClient.displayW = sr.getScaledWidth();
            FTBLibClient.displayH = sr.getScaledHeight();
        }

        if (e.phase == TickEvent.Phase.END && FTBLibClient.isIngame()) ClientNotifications.renderTemp();

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
        if (FTBLib.ftbu != null) FTBLib.ftbu.renderWorld(e.partialTicks);
    }
}
