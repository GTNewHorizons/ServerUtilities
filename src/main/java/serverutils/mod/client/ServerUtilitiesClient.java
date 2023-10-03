package serverutils.mod.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import serverutils.lib.client.BuiltinChunkMap;
import serverutils.lib.client.ClientConfig;
import serverutils.lib.client.ServerUtilitiesLibClientConfigManager;
import serverutils.lib.client.SidebarButtonManager;
import serverutils.lib.command.client.CommandClientConfig;
import serverutils.lib.command.client.CommandPrintItem;
import serverutils.lib.command.client.CommandPrintState;
import serverutils.lib.command.client.CommandSimulateButton;
import serverutils.lib.lib.client.ClientUtils;
import serverutils.lib.lib.client.ParticleColoredDust;
import serverutils.lib.lib.gui.misc.ChunkSelectorMap;
import serverutils.lib.lib.icon.PlayerHeadIcon;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.mod.ServerUtilities;
import serverutils.mod.ServerUtilitiesCommon;
import serverutils.mod.ServerUtilitiesConfig;
import serverutils.mod.handlers.ServerUtilitiesClientEventHandler;
import serverutils.utils.command.client.CommandKaomoji;
import serverutils.utils.command.client.CommandPing;

public class ServerUtilitiesClient extends ServerUtilitiesCommon {

    public static final Map<String, ClientConfig> CLIENT_CONFIG_MAP = new HashMap<>();
    public static KeyBinding KEY_NBT, KEY_TRASH;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        ServerUtilitiesClientConfig.init(event);
        ClientUtils.localPlayerHead = new PlayerHeadIcon(Minecraft.getMinecraft().getSession().func_148256_e().getId());
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(ServerUtilitiesLibClientConfigManager.INSTANCE);
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(SidebarButtonManager.INSTANCE);
        ChunkSelectorMap.setMap(new BuiltinChunkMap());

        if (System.getProperty("serverlibdev", "0").equals("1")) {
            Display.setTitle(
                    "[MC " + EnumChatFormatting.GOLD
                            + Loader.MC_VERSION
                            + EnumChatFormatting.WHITE
                            + " Dev :: "
                            + Minecraft.getMinecraft().getSession().getUsername());
        }
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesClientConfig.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesClientEventHandler.INST);
        FMLCommonHandler.instance().bus().register(ServerUtilitiesClientEventHandler.INST);

        ClientRegistry.registerKeyBinding(
                KEY_NBT = new KeyBinding("key.serverutilities.nbt", Keyboard.KEY_NONE, ServerUtilities.KEY_CATEGORY));
        ClientRegistry.registerKeyBinding(
                KEY_TRASH = new KeyBinding(
                        "key.serverutilities.trash",
                        Keyboard.KEY_NONE,
                        ServerUtilities.KEY_CATEGORY));

    }

    @Override
    public void postInit() {
        super.postInit();

        for (Map.Entry<String, String> entry : ServerUtilitiesCommon.KAOMOJIS.entrySet()) {
            ClientCommandHandler.instance.registerCommand(new CommandKaomoji(entry.getKey(), entry.getValue()));
        }
        ClientCommandHandler.instance.registerCommand(new CommandClientConfig());
        ClientCommandHandler.instance.registerCommand(new CommandSimulateButton());
        ClientCommandHandler.instance.registerCommand(new CommandPrintItem());
        ClientCommandHandler.instance.registerCommand(new CommandPrintState());
        // ClientCommandHandler.instance.registerCommand(new CommandListAchievements());
        ClientCommandHandler.instance.registerCommand(new CommandPing());

        // Minecraft.getMinecraft().getRenderManager().getSkinMap().get("default").addLayer(LayerBadge.INSTANCE);
        // Minecraft.getMinecraft().getRenderManager().getSkinMap().get("slim").addLayer(LayerBadge.INSTANCE);
    }

    @Override
    public void handleClientMessage(MessageToClient message) {
        if (ServerUtilitiesConfig.debugging.log_network) {
            ServerUtilities.LOGGER.info("Net RX: " + message.getClass().getName());
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
