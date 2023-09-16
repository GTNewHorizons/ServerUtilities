package ftb.lib.mod.client;

import java.util.List;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.FTBLib;
import ftb.lib.FTBWorld;
import ftb.lib.api.EventFTBWorldClient;
import ftb.lib.api.client.FTBLibClient;
import ftb.lib.api.item.LMInvUtils;
import ftb.lib.api.item.ODItems;

@SideOnly(Side.CLIENT)
public class FTBLibClientEventHandler {

    public static final FTBLibClientEventHandler instance = new FTBLibClientEventHandler();

    @SubscribeEvent
    public void onConnected(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        ServerData sd = FTBLibClient.mc.func_147104_D();
        EventFTBWorldClient event = new EventFTBWorldClient(null);
        if (FTBLib.ftbu != null) FTBLib.ftbu.onFTBWorldClient(event);
        FTBWorld.client = null;
        event.post();
    }

    @SubscribeEvent
    public void onDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        EventFTBWorldClient event = new EventFTBWorldClient(null);
        if (FTBLib.ftbu != null) FTBLib.ftbu.onFTBWorldClient(event);
        event.post();
        FTBWorld.client = null;
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent e) {
        if (e.itemStack == null || e.itemStack.getItem() == null) return;

        if (FTBLibModClient.item_reg_names.getAsBoolean()) {
            e.toolTip.add(LMInvUtils.getRegName(e.itemStack).toString());
        }

        if (FTBLibModClient.item_ore_names.getAsBoolean()) {
            List<String> ores = ODItems.getOreNames(e.itemStack);

            if (ores != null && !ores.isEmpty()) {
                e.toolTip.add("Ore Dictionary names:");
                for (String or : ores) e.toolTip.add("> " + or);
            }
        }

        if (FTBLib.ftbu != null) FTBLib.ftbu.onTooltip(e);
    }

    @SubscribeEvent
    public void onDrawDebugText(RenderGameOverlayEvent.Text e) {
        if (!FTBLibClient.mc.gameSettings.showDebugInfo) {
            if (FTBLib.DEV_ENV) {
                e.left.add(
                        "[MC " + EnumChatFormatting.GOLD + Loader.MC_VERSION + EnumChatFormatting.WHITE + " DevEnv]");
            }
        } else {
            // if(DevConsole.enabled()) e.left.add("r: " + MathHelperMC.get2DRotation(FTBLibClient.mc.thePlayer));
        }
    }

    @SubscribeEvent
    public void preTexturesLoaded(TextureStitchEvent.Pre e) {
        if (e.map.getTextureType() == 0) {
            FTBLibClient.blockNullIcon = e.map.registerIcon("ftbl:empty_block");
            FTBLibClient.clearCachedData();
        }
    }
}
