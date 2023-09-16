package serverutils.lib.mod.client;

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
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilsWorld;
import serverutils.lib.api.EventServerUtilitiesWorldClient;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.item.LMInvUtils;
import serverutils.lib.api.item.ODItems;

@SideOnly(Side.CLIENT)
public class ServerUtilitiesLibraryClientEventHandler {

    public static final ServerUtilitiesLibraryClientEventHandler instance = new ServerUtilitiesLibraryClientEventHandler();

    @SubscribeEvent
    public void onConnected(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        ServerData sd = ServerUtilitiesLibraryClient.mc.func_147104_D();
        EventServerUtilitiesWorldClient event = new EventServerUtilitiesWorldClient(null);
        if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
            ServerUtilitiesLib.serverUtilitiesIntegration.onServerUtilitiesWorldClient(event);
        ServerUtilsWorld.client = null;
        event.post();
    }

    @SubscribeEvent
    public void onDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        EventServerUtilitiesWorldClient event = new EventServerUtilitiesWorldClient(null);
        if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
            ServerUtilitiesLib.serverUtilitiesIntegration.onServerUtilitiesWorldClient(event);
        event.post();
        ServerUtilsWorld.client = null;
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent e) {
        if (e.itemStack == null || e.itemStack.getItem() == null) return;

        if (ServerUtilitiesLibraryModClient.item_reg_names.getAsBoolean()) {
            e.toolTip.add(LMInvUtils.getRegName(e.itemStack).toString());
        }

        if (ServerUtilitiesLibraryModClient.item_ore_names.getAsBoolean()) {
            List<String> ores = ODItems.getOreNames(e.itemStack);

            if (ores != null && !ores.isEmpty()) {
                e.toolTip.add("Ore Dictionary names:");
                for (String or : ores) e.toolTip.add("> " + or);
            }
        }

        if (ServerUtilitiesLib.serverUtilitiesIntegration != null)
            ServerUtilitiesLib.serverUtilitiesIntegration.onTooltip(e);
    }

    @SubscribeEvent
    public void onDrawDebugText(RenderGameOverlayEvent.Text e) {
        if (!ServerUtilitiesLibraryClient.mc.gameSettings.showDebugInfo) {
            if (ServerUtilitiesLib.DEV_ENV) {
                e.left.add(
                        "[MC " + EnumChatFormatting.GOLD + Loader.MC_VERSION + EnumChatFormatting.WHITE + " DevEnv]");
            }
        } else {
            // if(DevConsole.enabled()) e.left.add("r: " +
            // MathHelperMC.get2DRotation(ServerUtilitiesLibraryClient.mc.thePlayer));
        }
    }

    @SubscribeEvent
    public void preTexturesLoaded(TextureStitchEvent.Pre e) {
        if (e.map.getTextureType() == 0) {
            ServerUtilitiesLibraryClient.blockNullIcon = e.map.registerIcon("serverlib:empty_block");
            ServerUtilitiesLibraryClient.clearCachedData();
        }
    }
}
