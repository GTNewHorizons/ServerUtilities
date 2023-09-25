package serverutils.utils;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.util.IChatComponent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.lib.ATHelper;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.util.CommonUtils;
import serverutils.lib.lib.util.FileUtils;
import serverutils.lib.lib.util.SidedUtils;
import serverutils.utils.command.ServerUtilitiesCommands;
import serverutils.utils.ranks.CommandOverride;
import serverutils.utils.ranks.Rank;
import serverutils.utils.ranks.Ranks;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
        modid = ServerUtilities.MOD_ID,
        name = ServerUtilities.MOD_NAME,
        version = ServerUtilities.VERSION,
        acceptableRemoteVersions = "*",
        dependencies = ServerUtilitiesLib.THIS_DEP)
public class ServerUtilities {

    public static final String MOD_ID = "ftbutilities";
    public static final String MOD_NAME = "FTB Utilities";
    public static final String VERSION = "GRADLETOKEN_VERSION";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @Mod.Instance(MOD_ID)
    public static ServerUtilities INST;

    @SidedProxy(
            serverSide = "serverutils.utils.ServerUtilitiesCommon",
            clientSide = "serverutils.utils.client.ServerUtilitiesClient")
    public static ServerUtilitiesCommon PROXY;

    public static IChatComponent lang(@Nullable ICommandSender sender, String key, Object... args) {
        return SidedUtils.lang(sender, MOD_ID, key, args);
    }

    public static CommandException error(@Nullable ICommandSender sender, String key, Object... args) {
        return CommandUtils.error(lang(sender, key, args));
    }

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        PROXY.preInit(event);
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        PROXY.init();
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {
        PROXY.postInit();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        ServerUtilitiesCommands.registerCommands(event);
    }

    @Mod.EventHandler
    public void onIMC(FMLInterModComms.IMCEvent event) {
        for (FMLInterModComms.IMCMessage message : event.getMessages()) {
            PROXY.imc(message);
        }
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        if (Ranks.isActive()) {
            Ranks.INSTANCE.commands.clear();
            FileUtils.deleteSafe(Ranks.INSTANCE.universe.server.getFile("local/serverutilities/all_permissions.html"));
            FileUtils.deleteSafe(Ranks.INSTANCE.universe.server.getFile("local/serverutilities/all_permissions_full_list.txt"));

            boolean spongeLoaded = Loader.isModLoaded("spongeforge");

            if (spongeLoaded) {
                LOGGER.warn(
                        "Sponge detected, command overriding has been disabled. If there are any issues with Server Utilities ranks or permissions, please test them without Sponge!");
            }

            if (!ServerUtilitiesConfig.ranks.override_commands || spongeLoaded) {
                return;
            }

            ServerCommandManager manager = (ServerCommandManager) Ranks.INSTANCE.universe.server.getCommandManager();
            List<ICommand> commands = new ArrayList<>(manager.getCommands().values());
            ATHelper.getCommandSet(manager).clear();
            manager.getCommands().clear();

            for (ICommand command : commands) {
                ModContainer container = CommonUtils.getModContainerForClass(command.getClass());
                manager.registerCommand(
                        CommandOverride.create(
                                command,
                                container == null ? Rank.NODE_COMMAND
                                        : (Rank.NODE_COMMAND + '.' + container.getModId()),
                                container));
            }

            List<CommandOverride> ocommands = new ArrayList<>(Ranks.INSTANCE.commands.values());
            ocommands.sort((o1, o2) -> {
                int i = Boolean.compare(o1.modContainer != null, o2.modContainer != null);
                return i == 0 ? o1.node.compareTo(o2.node) : i;
            });

            for (CommandOverride c : ocommands) {
                Ranks.INSTANCE.commands.put(c.node, c);
            }

            LOGGER.info("Overridden " + manager.getCommands().size() + " commands");
        }
    }
}
