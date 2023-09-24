package serverutils.serverlib.client;

import cpw.mods.fml.common.Loader;
import net.minecraft.util.EnumChatFormatting;
import serverutils.serverlib.ServerLib;
import serverutils.serverlib.ServerLibCommon;
import serverutils.serverlib.ServerLibConfig;
import serverutils.serverlib.command.client.CommandClientConfig;
import serverutils.serverlib.command.client.CommandListAchievements;
import serverutils.serverlib.command.client.CommandPrintItem;
import serverutils.serverlib.command.client.CommandPrintState;
import serverutils.serverlib.command.client.CommandSimulateButton;
import serverutils.serverlib.lib.client.ClientUtils;
import serverutils.serverlib.lib.client.ParticleColoredDust;
import serverutils.serverlib.lib.gui.misc.ChunkSelectorMap;
import serverutils.serverlib.lib.icon.PlayerHeadIcon;
import serverutils.serverlib.lib.net.MessageToClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.opengl.Display;

import java.util.HashMap;
import java.util.Map;

public class ServerLibClient extends ServerLibCommon {
	public static final Map<String, ClientConfig> CLIENT_CONFIG_MAP = new HashMap<>();

	@Override
	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);
		ServerLibClientConfig.sync();
		ClientUtils.localPlayerHead = new PlayerHeadIcon(Minecraft.getMinecraft().getSession().func_148256_e().getId());
		((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(ServerLibClientConfigManager.INSTANCE);
		((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(SidebarButtonManager.INSTANCE);
		ChunkSelectorMap.setMap(new BuiltinChunkMap());

		if (System.getProperty("serverlibdevenvironment", "0").equals("1")) {
			Display.setTitle("[MC " + EnumChatFormatting.GOLD + Loader.MC_VERSION + EnumChatFormatting.WHITE + " Dev :: " + Minecraft.getMinecraft().getSession().getUsername());
		}
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
	public void handleClientMessage(MessageToClient message)
	{
		if (ServerLibConfig.debugging.log_network)
		{
			ServerLib.LOGGER.info("Net RX: " + message.getClass().getName());
		}

		message.onMessage();
	}

	@Override
	public void spawnDust(World world, double x, double y, double z, float r, float g, float b, float a)
	{
		ClientUtils.spawnParticle(new ParticleColoredDust(world, x, y, z, r, g, b, a));
	}

	@Override
	public long getWorldTime()
	{
		return Minecraft.getMinecraft().theWorld == null ? super.getWorldTime() : Minecraft.getMinecraft().theWorld.getTotalWorldTime();
	}
}