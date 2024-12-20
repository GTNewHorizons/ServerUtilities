package serverutils;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import serverutils.lib.command.CommandUtils;

@Mod(
        modid = ServerUtilities.MOD_ID,
        name = ServerUtilities.MOD_NAME,
        version = ServerUtilities.VERSION,
        dependencies = "required-after:gtnhlib;" + "after:navigator;",
        guiFactory = "serverutils.client.gui.GuiFactory")
public class ServerUtilities {

    public static final String MOD_ID = "serverutilities";
    public static final String MOD_NAME = "Server Utilities";
    public static final String VERSION = Tags.GRADLETOKEN_VERSION;
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static final String SERVER_FOLDER = MOD_ID + "/server/";

    @Mod.Instance(MOD_ID)
    public static ServerUtilities INST;

    @SidedProxy(
            serverSide = "serverutils.ServerUtilitiesCommon",
            clientSide = "serverutils.client.ServerUtilitiesClient")
    public static ServerUtilitiesCommon PROXY;

    public static IChatComponent lang(@Nullable ICommandSender sender, String key, Object... args) {
        return lang(key, args);
    }

    public static IChatComponent lang(String key, Object... args) {
        return new ChatComponentTranslation(key, args);
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
        PROXY.postInit(event);
    }

    @Mod.EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        PROXY.onServerAboutToStart(event);
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        PROXY.onServerStarting(event);
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        PROXY.onServerStarted(event);
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        PROXY.onServerStopping(event);
    }

    @NetworkCheckHandler
    public boolean checkModLists(Map<String, String> map, Side side) {
        return side != Side.CLIENT || map.containsKey(MOD_ID) && map.get(MOD_ID).equals(VERSION);
    }
}
