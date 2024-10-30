package serverutils;

import static serverutils.ServerUtilitiesConfig.auto_shutdown;
import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesConfig.ranks;
import static serverutils.ServerUtilitiesConfig.tasks;
import static serverutils.ServerUtilitiesConfig.world;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import serverutils.aurora.Aurora;
import serverutils.aurora.AuroraConfig;
import serverutils.command.ServerUtilitiesCommands;
import serverutils.data.ServerUtilitiesLoadedChunkManager;
import serverutils.events.ServerReloadEvent;
import serverutils.lib.OtherMods;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.IConfigCallback;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.net.ServerUtilitiesNetHandler;
import serverutils.ranks.ServerUtilitiesPermissionHandler;
import serverutils.task.CleanupTask;
import serverutils.task.DecayTask;
import serverutils.task.ShutdownTask;
import serverutils.task.backup.BackupTask;

public class ServerUtilitiesCommon {

    public static final Map<String, String> KAOMOJIS = new HashMap<>();
    public static final Map<UUID, ServerUtilitiesCommon.EditingConfig> TEMP_SERVER_CONFIG = new HashMap<>();
    private static final Map<String, Function<ForgePlayer, IChatComponent>> CHAT_FORMATTING_SUBSTITUTES = new HashMap<>();

    public static Function<String, IChatComponent> chatFormattingSubstituteFunction(ForgePlayer player) {
        return s -> {
            Function<ForgePlayer, IChatComponent> sub = CHAT_FORMATTING_SUBSTITUTES.get(s);
            return sub == null ? null : sub.apply(player);
        };
    }

    public static class EditingConfig {

        public final ConfigGroup group;
        public final IConfigCallback callback;

        public EditingConfig(ConfigGroup g, IConfigCallback c) {
            group = g;
            callback = c;
        }
    }

    public void preInit(FMLPreInitializationEvent event) {
        OtherMods.init();
        if (ranks.enabled) {
            PermissionAPI.setPermissionHandler(ServerUtilitiesPermissionHandler.INSTANCE);
        }

        ServerUtilitiesStats.init();
        ServerUtilitiesNetHandler.init();

        if (!ForgeChunkManager.getConfig().hasCategory(ServerUtilities.MOD_ID)) {
            ForgeChunkManager.getConfig().get(ServerUtilities.MOD_ID, "maximumChunksPerTicket", 1000000).setMinValue(0);
            ForgeChunkManager.getConfig().get(ServerUtilities.MOD_ID, "maximumTicketCount", 1000000).setMinValue(0);
            ForgeChunkManager.getConfig().save();
        }

        ForgeChunkManager
                .setForcedChunkLoadingCallback(ServerUtilities.INST, ServerUtilitiesLoadedChunkManager.INSTANCE);

        KAOMOJIS.put("shrug", "\u00AF\\_(\u30C4)_/\u00AF");
        KAOMOJIS.put("tableflip", "(\u256F\u00B0\u25A1\u00B0)\u256F \uFE35 \u253B\u2501\u253B");
        KAOMOJIS.put("unflip", "\u252C\u2500\u252C\u30CE( \u309C-\u309C\u30CE)");
    }

    public void init(FMLInitializationEvent event) {
        ServerUtilitiesRegistry.registerDefaults();
        ServerUtilitiesPermissions.init();
        CHAT_FORMATTING_SUBSTITUTES.put("name", ForgePlayer::getDisplayName);
        CHAT_FORMATTING_SUBSTITUTES.put("team", player -> player.team.getTitle());
    }

    public void postInit(FMLPostInitializationEvent event) {
        if ((Loader.isModLoaded("FTBU") || Loader.isModLoaded("FTBL")) && event.getSide().isServer()) {
            throw new RuntimeException("FTBU/FTBL Detected, please remove them and start again.");
        }
    }

    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        Universe.onServerAboutToStart(event);
        MinecraftForge.EVENT_BUS.register(Universe.get());
        FMLCommonHandler.instance().bus().register(Universe.get());
    }

    public void onServerStarting(FMLServerStartingEvent event) {
        ServerUtilitiesCommands.registerCommands(event);

        if (AuroraConfig.general.enable) {
            Aurora.start(event.getServer());
        }
    }

    public void onServerStarted(FMLServerStartedEvent event) {
        Universe.onServerStarted(event);
        registerTasks();
    }

    public void onServerStopping(FMLServerStoppingEvent event) {
        // Save the universe since onServerStopping clears the instance variable
        Universe oldUniverse = Universe.get();
        Universe.onServerStopping(event);
        Aurora.stop();
        MinecraftForge.EVENT_BUS.unregister(oldUniverse);
        FMLCommonHandler.instance().bus().unregister(oldUniverse);
    }

    public void registerTasks() {
        Universe universe = Universe.get();
        universe.scheduleTask(new DecayTask(), world.chunk_claiming);
        universe.scheduleTask(new CleanupTask(), tasks.cleanup.enabled);
        universe.scheduleTask(new BackupTask(), backups.enable_backups);
        if (auto_shutdown.enabled && auto_shutdown.times.length > 0
                && (auto_shutdown.enabled_singleplayer || universe.server.isDedicatedServer())) {
            universe.scheduleTask(new ShutdownTask());
        }
    }

    static boolean onReload(ServerReloadEvent event) {
        if (event.getUniverse() != null) {
            ServerUtilitiesLeaderboards.loadLeaderboards();
        }
        return true;
    }

    public void handleClientMessage(MessageToClient message) {}

    public long getWorldTime() {
        return ServerUtils.getServerWorld().getTotalWorldTime();
    }
}
