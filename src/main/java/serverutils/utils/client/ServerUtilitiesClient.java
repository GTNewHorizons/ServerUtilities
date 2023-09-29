package serverutils.utils.client;

import java.util.Map;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.utils.ServerUtilitiesCommon;
import serverutils.utils.command.client.CommandKaomoji;
import serverutils.utils.command.client.CommandPing;
import serverutils.utils.handlers.ServerUtilitiesClientEventHandler;

public class ServerUtilitiesClient extends ServerUtilitiesCommon // ServerUtilitiesLibClient
{

    public static KeyBinding KEY_NBT, KEY_TRASH;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        ServerUtilitiesClientConfig.init(event);
        ClientRegistry.registerKeyBinding(
                KEY_NBT = new KeyBinding(
                        "key.serverutilities.nbt",
                        Keyboard.KEY_NONE,
                        ServerUtilitiesLib.KEY_CATEGORY));
        ClientRegistry.registerKeyBinding(
                KEY_TRASH = new KeyBinding(
                        "key.serverutilities.trash",
                        Keyboard.KEY_NONE,
                        ServerUtilitiesLib.KEY_CATEGORY));

        MinecraftForge.EVENT_BUS.register(ServerUtilitiesClientEventHandler.INST);
    }

    @Override
    public void postInit() {
        super.postInit();

        for (Map.Entry<String, String> entry : ServerUtilitiesCommon.KAOMOJIS.entrySet()) {
            ClientCommandHandler.instance.registerCommand(new CommandKaomoji(entry.getKey(), entry.getValue()));
        }

        ClientCommandHandler.instance.registerCommand(new CommandPing());

        // Minecraft.getMinecraft().getRenderManager().getSkinMap().get("default").addLayer(LayerBadge.INSTANCE);
        // Minecraft.getMinecraft().getRenderManager().getSkinMap().get("slim").addLayer(LayerBadge.INSTANCE);
    }
}
