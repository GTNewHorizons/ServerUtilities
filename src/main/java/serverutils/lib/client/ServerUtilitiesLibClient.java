package serverutils.lib.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.Display;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilitiesLibCommon;
import serverutils.lib.ServerUtilitiesLibConfig;
import serverutils.lib.command.client.CommandClientConfig;
import serverutils.lib.command.client.CommandListAchievements;
import serverutils.lib.command.client.CommandPrintItem;
import serverutils.lib.command.client.CommandPrintState;
import serverutils.lib.command.client.CommandSimulateButton;
import serverutils.lib.lib.client.ClientUtils;
import serverutils.lib.lib.client.ParticleColoredDust;
import serverutils.lib.lib.gui.misc.ChunkSelectorMap;
import serverutils.lib.lib.icon.PlayerHeadIcon;
import serverutils.lib.lib.net.MessageToClient;

public class ServerUtilitiesLibClient extends ServerUtilitiesLibCommon {

    public static final Map<String, ClientConfig> CLIENT_CONFIG_MAP = new HashMap<>();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ServerUtilitiesLibClientConfig.init(event);
        ClientUtils.localPlayerHead = new PlayerHeadIcon(Minecraft.getMinecraft().getSession().func_148256_e().getId());
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(ServerUtilitiesLibClientConfigManager.INSTANCE);
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(SidebarButtonManager.INSTANCE);
        ChunkSelectorMap.setMap(new BuiltinChunkMap());

        if (System.getProperty("ServerLibDev", "0").equals("1")) {
            Display.setTitle(
                    "[MC " + EnumChatFormatting.GOLD
                            + Loader.MC_VERSION
                            + EnumChatFormatting.WHITE
                            + " Dev :: "
                            + Minecraft.getMinecraft().getSession().getUsername());
        }
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesLibClientConfig.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesLibClientEventHandler.INST);
        FMLCommonHandler.instance().bus().register(ServerUtilitiesLibClientEventHandler.INST);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    @Override
    public void postInit() {
        super.postInit();

        ClientCommandHandler.instance.registerCommand(new CommandClientConfig());
        ClientCommandHandler.instance.registerCommand(new CommandSimulateButton());
        ClientCommandHandler.instance.registerCommand(new CommandPrintItem());
        ClientCommandHandler.instance.registerCommand(new CommandPrintState());
        ClientCommandHandler.instance.registerCommand(new CommandListAchievements());
    }

    @Override
    public void handleClientMessage(MessageToClient message) {
        if (ServerUtilitiesLibConfig.debugging.log_network) {
            ServerUtilitiesLib.LOGGER.info("Net RX: " + message.getClass().getName());
        }

        message.onMessage();
    }

    @Override
    public void spawnDust(World world, double x, double y, double z, float r, float g, float b, float a) {
        ClientUtils.spawnParticle(new ParticleColoredDust(world, x, y, z, r, g, b, a));
    }

    @Override
    public long getWorldTime() {
        return Minecraft.getMinecraft().theWorld == null ? super.getWorldTime()
                : Minecraft.getMinecraft().theWorld.getTotalWorldTime();
    }
}
