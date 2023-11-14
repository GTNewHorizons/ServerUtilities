package serverutils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import serverutils.aurora.AuroraConfig;
import serverutils.aurora.mc.AuroraMinecraftHandler;
import serverutils.backups.Backups;
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
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.icon.Color4I;
import serverutils.lib.math.Ticks;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.util.InvUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.net.ServerUtilitiesNetHandler;
import serverutils.ranks.ServerUtilitiesPermissionHandler;

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
    public static boolean isVPLoaded;

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
        if ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
            ServerUtilities.LOGGER.info("Loading ServerUtilities in development environment");
        }

        isVPLoaded = Loader.isModLoaded(OtherMods.VP);

        ServerUtilitiesConfig.init(event);
        AuroraConfig.init(event);

        if (ServerUtilitiesConfig.ranks.enabled) {
            PermissionAPI.setPermissionHandler(ServerUtilitiesPermissionHandler.INSTANCE);
        }

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

        Backups.init();

        MinecraftForge.EVENT_BUS.register(ServerUtilitiesPlayerEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesRegistryEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesServerEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesWorldEventHandler.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesUniverseData.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesPermissions.INST);
        MinecraftForge.EVENT_BUS.register(ServerUtilitiesLeaderboards.INST);
        FMLCommonHandler.instance().bus().register(ServerUtilitiesServerEventHandler.INST);
        if (AuroraConfig.general.enable) {
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

    public void imc(FMLInterModComms.IMCMessage message) {}

    public void handleClientMessage(MessageToClient message) {}

    public void spawnDust(World world, double x, double y, double z, float r, float g, float b, float a) {}

    public void spawnDust(World world, double x, double y, double z, Color4I col) {
        spawnDust(world, x, y, z, col.redf(), col.greenf(), col.bluef(), col.alphaf());
    }

    public long getWorldTime() {
        return ServerUtils.getServerWorld().getTotalWorldTime();
    }
}
