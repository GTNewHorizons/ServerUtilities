package serverutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import serverutils.aurora.Aurora;
import serverutils.aurora.AuroraConfig;
import serverutils.command.CmdAddFakePlayer;
import serverutils.command.CmdMySettings;
import serverutils.command.CmdReload;
import serverutils.command.ServerUtilitiesCommands;
import serverutils.command.team.CmdTeam;
import serverutils.lib.ATHelper;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.Universe;
import serverutils.lib.util.CommonUtils;
import serverutils.lib.util.SidedUtils;
import serverutils.ranks.CommandOverride;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;

@Mod(
        modid = ServerUtilities.MOD_ID,
        name = ServerUtilities.MOD_NAME,
        version = ServerUtilities.VERSION,
        acceptableRemoteVersions = "*",
        dependencies = "")
public class ServerUtilities {

    public static final String MOD_ID = "serverutilities";
    public static final String MOD_NAME = "Server Utilities";
    public static final String VERSION = "GRADLETOKEN_VERSION";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static final String SERVER_FOLDER = MOD_ID + "/server/";

    @Mod.Instance(MOD_ID)
    public static ServerUtilities INST;

    @SidedProxy(
            serverSide = "serverutils.ServerUtilitiesCommon",
            clientSide = "serverutils.client.ServerUtilitiesClient")
    public static ServerUtilitiesCommon PROXY;

    public static IChatComponent lang(@Nullable ICommandSender sender, String key, Object... args) {
        return SidedUtils.lang(sender, MOD_ID, key, args);
    }

    public static CommandException error(@Nullable ICommandSender sender, String key, Object... args) {
        return CommandUtils.error(lang(sender, key, args));
    }

    public static CommandException errorFeatureDisabledServer(@Nullable ICommandSender sender) {
        return error(sender, "feature_disabled_server");
    }

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        Locale.setDefault(Locale.US);
        PROXY.preInit(event);
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        PROXY.init(event);
    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {
        PROXY.postInit();
    }

    @Mod.EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        Universe.onServerAboutToStart(event);
        MinecraftForge.EVENT_BUS.register(Universe.get());
        FMLCommonHandler.instance().bus().register(Universe.get());
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        ServerUtilitiesCommands.registerCommands(event);
        event.registerServerCommand(new CmdReload());
        event.registerServerCommand(new CmdMySettings());
        event.registerServerCommand(new CmdTeam());

        if (ServerUtilitiesConfig.debugging.special_commands) {
            event.registerServerCommand(new CmdAddFakePlayer());
        }
        if (AuroraConfig.general.enable) {
            Aurora.start(event.getServer());
        }
    }

    @Mod.EventHandler
    public void onIMC(FMLInterModComms.IMCEvent event) {
        for (FMLInterModComms.IMCMessage message : event.getMessages()) {
            PROXY.imc(message);
        }
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        Universe.onServerStarted(event);

        if (Ranks.isActive()) {
            Ranks.INSTANCE.commands.clear();

            boolean crucibleLoaded = Loader.isModLoaded("Crucible");

            if (crucibleLoaded) {
                LOGGER.warn(
                        "Crucible detected, command overriding has been disabled. If there are any issues with Server Utilities ranks or permissions, please test them without Crucible!");
            }

            if (!ServerUtilitiesConfig.ranks.override_commands || crucibleLoaded) {
                return;
            }

            ServerCommandManager manager = (ServerCommandManager) Ranks.INSTANCE.universe.server.getCommandManager();
            List<ICommand> commands = new ArrayList<>(ATHelper.getCommandSet(manager));
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

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        Universe.onServerStopping(event);
        Aurora.stop();
    }

    @NetworkCheckHandler
    public boolean checkModLists(Map<String, String> map, Side side) {
        SidedUtils.checkModLists(side, map);
        return true;
    }
}
