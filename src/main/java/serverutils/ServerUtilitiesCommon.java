package serverutils;

import static serverutils.ServerUtilitiesConfig.auto_shutdown;
import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesConfig.ranks;
import static serverutils.ServerUtilitiesConfig.tasks;
import static serverutils.ServerUtilitiesConfig.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.command.ICommand;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import serverutils.aurora.Aurora;
import serverutils.aurora.AuroraConfig;
import serverutils.aurora.mc.AuroraMinecraftHandler;
import serverutils.command.ServerUtilitiesCommands;
import serverutils.data.Leaderboard;
import serverutils.data.NodeEntry;
import serverutils.data.ServerUtilitiesLoadedChunkManager;
import serverutils.data.ServerUtilitiesUniverseData;
import serverutils.events.CustomPermissionPrefixesRegistryEvent;
import serverutils.events.IReloadHandler;
import serverutils.events.LeaderboardRegistryEvent;
import serverutils.events.ServerReloadEvent;
import serverutils.events.ServerUtilitiesPreInitRegistryEvent;
import serverutils.handlers.ServerUtilitiesPlayerEventHandler;
import serverutils.handlers.ServerUtilitiesRegistryEventHandler;
import serverutils.handlers.ServerUtilitiesServerEventHandler;
import serverutils.handlers.ServerUtilitiesWorldEventHandler;
import serverutils.lib.ATHelper;
import serverutils.lib.EnumReloadType;
import serverutils.lib.OtherMods;
import serverutils.lib.config.ConfigBoolean;
import serverutils.lib.config.ConfigColor;
import serverutils.lib.config.ConfigDouble;
import serverutils.lib.config.ConfigEnum;
import serverutils.lib.config.ConfigFluid;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.ConfigInt;
import serverutils.lib.config.ConfigItemStack;
import serverutils.lib.config.ConfigList;
import serverutils.lib.config.ConfigLong;
import serverutils.lib.config.ConfigNBT;
import serverutils.lib.config.ConfigNull;
import serverutils.lib.config.ConfigString;
import serverutils.lib.config.ConfigStringEnum;
import serverutils.lib.config.ConfigTeam;
import serverutils.lib.config.ConfigTeamClient;
import serverutils.lib.config.ConfigTextComponent;
import serverutils.lib.config.ConfigTimer;
import serverutils.lib.config.ConfigValueProvider;
import serverutils.lib.config.IConfigCallback;
import serverutils.lib.config.RankConfigAPI;
import serverutils.lib.data.AdminPanelAction;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ISyncData;
import serverutils.lib.data.ServerUtilitiesAPI;
import serverutils.lib.data.ServerUtilitiesTeamGuiActions;
import serverutils.lib.data.TeamAction;
import serverutils.lib.data.Universe;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.icon.Color4I;
import serverutils.lib.math.Ticks;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.util.CommonUtils;
import serverutils.lib.util.InvUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.net.ServerUtilitiesNetHandler;
import serverutils.ranks.CommandOverride;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;
import serverutils.ranks.ServerUtilitiesPermissionHandler;
import serverutils.task.CleanupTask;
import serverutils.task.DecayTask;
import serverutils.task.ShutdownTask;
import serverutils.task.backup.BackupTask;

public class ServerUtilitiesCommon {

    public static final Collection<NodeEntry> CUSTOM_PERM_PREFIX_REGISTRY = new HashSet<>();
    public static final Map<ResourceLocation, Leaderboard> LEADERBOARDS = new HashMap<>();
    public static final Map<String, String> KAOMOJIS = new HashMap<>();
    public static final Map<String, ConfigValueProvider> CONFIG_VALUE_PROVIDERS = new HashMap<>();
    public static final Map<UUID, ServerUtilitiesCommon.EditingConfig> TEMP_SERVER_CONFIG = new HashMap<>();
    public static final Map<String, ISyncData> SYNCED_DATA = new HashMap<>();
    public static final HashMap<ResourceLocation, IReloadHandler> RELOAD_IDS = new HashMap<>();
    public static final Map<ResourceLocation, TeamAction> TEAM_GUI_ACTIONS = new HashMap<>();
    public static final Map<ResourceLocation, AdminPanelAction> ADMIN_PANEL_ACTIONS = new HashMap<>();
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

    static {
        try {
            ConfigurationManager.registerConfig(ServerUtilitiesConfig.class);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }

    public void preInit(FMLPreInitializationEvent event) {
        if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
            ServerUtilities.LOGGER.info("Loading ServerUtilities in development environment");
        }

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

        MinecraftForge.EVENT_BUS.register(ServerUtilitiesPlayerEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesRegistryEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesServerEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesWorldEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesUniverseData.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesPermissions.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesLeaderboards.INST);
        FMLCommonHandler.instance().bus().register(ServerUtilitiesServerEventHandler.INST);
        if (AuroraConfig.enable) {
            MinecraftForge.EVENT_BUS.register(AuroraMinecraftHandler.INST);
            FMLCommonHandler.instance().bus().register(AuroraMinecraftHandler.INST);
        }
        new CustomPermissionPrefixesRegistryEvent(CUSTOM_PERM_PREFIX_REGISTRY::add).post();
    }

    public void init(FMLInitializationEvent event) {
        new LeaderboardRegistryEvent(leaderboard -> LEADERBOARDS.put(leaderboard.id, leaderboard)).post();
        ServerUtilitiesPermissions.registerPermissions();

        ServerUtilitiesPreInitRegistryEvent.Registry registry = new ServerUtilitiesPreInitRegistryEvent.Registry() {

            @Override
            public void registerConfigValueProvider(String id, ConfigValueProvider provider) {
                CONFIG_VALUE_PROVIDERS.put(id, provider);
            }

            @Override
            public void registerSyncData(String mod, ISyncData data) {
                SYNCED_DATA.put(mod, data);
            }

            @Override
            public void registerServerReloadHandler(ResourceLocation id, IReloadHandler handler) {
                RELOAD_IDS.put(id, handler);
            }

            @Override
            public void registerAdminPanelAction(AdminPanelAction action) {
                ADMIN_PANEL_ACTIONS.put(action.getId(), action);
            }

            @Override
            public void registerTeamAction(TeamAction action) {
                TEAM_GUI_ACTIONS.put(action.getId(), action);
            }
        };

        registry.registerConfigValueProvider(ConfigNull.ID, () -> ConfigNull.INSTANCE);
        registry.registerConfigValueProvider(ConfigList.ID, () -> new ConfigList<>(ConfigNull.INSTANCE));
        registry.registerConfigValueProvider(ConfigBoolean.ID, () -> new ConfigBoolean(false));
        registry.registerConfigValueProvider(ConfigInt.ID, () -> new ConfigInt(0));
        registry.registerConfigValueProvider(ConfigDouble.ID, () -> new ConfigDouble(0D));
        registry.registerConfigValueProvider(ConfigLong.ID, () -> new ConfigLong(0L));
        registry.registerConfigValueProvider(ConfigString.ID, () -> new ConfigString(""));
        registry.registerConfigValueProvider(ConfigColor.ID, () -> new ConfigColor(Color4I.WHITE));
        registry.registerConfigValueProvider(ConfigEnum.ID, () -> new ConfigStringEnum(Collections.emptyList(), ""));
        registry.registerConfigValueProvider(ConfigItemStack.ID, () -> new ConfigItemStack(InvUtils.EMPTY_STACK));
        registry.registerConfigValueProvider(
                ConfigTextComponent.ID,
                () -> new ConfigTextComponent(new ChatComponentText("")));
        registry.registerConfigValueProvider(ConfigTimer.ID, () -> new ConfigTimer(Ticks.NO_TICKS));
        registry.registerConfigValueProvider(ConfigNBT.ID, () -> new ConfigNBT(null));
        registry.registerConfigValueProvider(ConfigFluid.ID, () -> new ConfigFluid(null, null));
        registry.registerConfigValueProvider(ConfigTeam.TEAM_ID, () -> new ConfigTeamClient(""));

        registry.registerAdminPanelAction(
                new AdminPanelAction(ServerUtilities.MOD_ID, "reload", GuiIcons.REFRESH, -1000) {

                    @Override
                    public Type getType(ForgePlayer player, NBTTagCompound data) {
                        return Type.fromBoolean(player.isOP());
                    }

                    @Override
                    public void onAction(ForgePlayer player, NBTTagCompound data) {
                        ServerUtilitiesAPI.reloadServer(
                                player.team.universe,
                                player.getPlayer(),
                                EnumReloadType.RELOAD_COMMAND,
                                ServerReloadEvent.ALL);
                    }
                }.setTitle(new ChatComponentTranslation("serverutilities.lang.reload_server_button")));

        registry.registerTeamAction(ServerUtilitiesTeamGuiActions.CONFIG);
        registry.registerTeamAction(ServerUtilitiesTeamGuiActions.INFO);
        registry.registerTeamAction(ServerUtilitiesTeamGuiActions.MEMBERS);
        registry.registerTeamAction(ServerUtilitiesTeamGuiActions.ALLIES);
        registry.registerTeamAction(ServerUtilitiesTeamGuiActions.MODERATORS);
        registry.registerTeamAction(ServerUtilitiesTeamGuiActions.ENEMIES);
        registry.registerTeamAction(ServerUtilitiesTeamGuiActions.LEAVE);
        registry.registerTeamAction(ServerUtilitiesTeamGuiActions.TRANSFER_OWNERSHIP);

        new ServerUtilitiesPreInitRegistryEvent(registry).post();

        RankConfigAPI.getHandler();

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

        if (AuroraConfig.enable) {
            Aurora.start(event.getServer());
        }
    }

    public void onServerStarted(FMLServerStartedEvent event) {
        Universe.onServerStarted(event);
        registerTasks();

        if (Ranks.isActive()) {
            Ranks.INSTANCE.commands.clear();

            boolean bukkitLoaded = CommonUtils.getClassExists("thermos.ThermosRemapper")
                    || CommonUtils.getClassExists("org.ultramine.server.UltraminePlugin")
                    || CommonUtils.getClassExists("org.bukkit.World");

            if (bukkitLoaded) {
                ServerUtilities.LOGGER.warn(
                        "Thermos/Ultramine detected, command overriding has been disabled. If there are any issues with Server Utilities ranks or permissions, please test them without those mods!");
            }

            if (!ranks.override_commands || bukkitLoaded) {
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

            ServerUtilities.LOGGER.info("Overridden {} commands", manager.getCommands().size());
        }
    }

    public void onServerStopping(FMLServerStoppingEvent event) {
        Universe.onServerStopping(event);
        Aurora.stop();
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

    public void handleClientMessage(MessageToClient message) {}

    public long getWorldTime() {
        return ServerUtils.getServerWorld().getTotalWorldTime();
    }
}
