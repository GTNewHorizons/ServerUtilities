package serverutils;

import static serverutils.ServerUtilitiesPermissions.RANK_EDIT;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameRules;

import serverutils.events.IReloadHandler;
import serverutils.events.ServerReloadEvent;
import serverutils.handlers.ServerUtilitiesSyncData;
import serverutils.lib.EnumReloadType;
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
import serverutils.lib.data.AdminPanelAction;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ISyncData;
import serverutils.lib.data.ServerUtilitiesAPI;
import serverutils.lib.data.ServerUtilitiesTeamGuiActions;
import serverutils.lib.data.TeamAction;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.ItemIcon;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.InvUtils;
import serverutils.lib.util.StringUtils;
import serverutils.net.MessageRanks;
import serverutils.net.MessageViewCrashList;
import serverutils.ranks.Ranks;

public class ServerUtilitiesRegistry {

    public static final Map<ResourceLocation, IReloadHandler> RELOAD_IDS = new HashMap<>();
    public static final Map<ResourceLocation, TeamAction> TEAM_GUI_ACTIONS = new HashMap<>();
    public static final Map<ResourceLocation, AdminPanelAction> ADMIN_PANEL_ACTIONS = new HashMap<>();
    public static final Map<String, ConfigValueProvider> CONFIG_VALUE_PROVIDERS = new HashMap<>();
    public static final Map<String, ISyncData> SYNCED_DATA = new HashMap<>();

    public static void registerConfigValueProvider(String id, ConfigValueProvider provider) {
        CONFIG_VALUE_PROVIDERS.put(id, provider);
    }

    public static void registerSyncData(String mod, ISyncData data) {
        SYNCED_DATA.put(mod, data);
    }

    public static void registerServerReloadHandler(ResourceLocation id, IReloadHandler handler) {
        RELOAD_IDS.put(id, handler);
    }

    public static void registerAdminPanelAction(AdminPanelAction action) {
        ADMIN_PANEL_ACTIONS.put(action.getId(), action);
    }

    public static void registerTeamAction(TeamAction action) {
        TEAM_GUI_ACTIONS.put(action.getId(), action);
    }

    static void registerDefaults() {
        registerDefaultConfigProviders();
        registerDefaultTeamActions();
        registerDefaultReloadHandlers();
        registerDefaultSyncData();
        registerDefaultAdminPanelActions();
    }

    private static void registerDefaultConfigProviders() {
        registerConfigValueProvider(ConfigNull.ID, () -> ConfigNull.INSTANCE);
        registerConfigValueProvider(ConfigList.ID, () -> new ConfigList<>(ConfigNull.INSTANCE));
        registerConfigValueProvider(ConfigBoolean.ID, () -> new ConfigBoolean(false));
        registerConfigValueProvider(ConfigInt.ID, () -> new ConfigInt(0));
        registerConfigValueProvider(ConfigDouble.ID, () -> new ConfigDouble(0D));
        registerConfigValueProvider(ConfigLong.ID, () -> new ConfigLong(0L));
        registerConfigValueProvider(ConfigString.ID, () -> new ConfigString(""));
        registerConfigValueProvider(ConfigColor.ID, () -> new ConfigColor(Color4I.WHITE));
        registerConfigValueProvider(ConfigEnum.ID, () -> new ConfigStringEnum(Collections.emptyList(), ""));
        registerConfigValueProvider(ConfigItemStack.ID, () -> new ConfigItemStack(InvUtils.EMPTY_STACK));
        registerConfigValueProvider(ConfigTextComponent.ID, () -> new ConfigTextComponent(new ChatComponentText("")));
        registerConfigValueProvider(ConfigTimer.ID, () -> new ConfigTimer(Ticks.NO_TICKS));
        registerConfigValueProvider(ConfigNBT.ID, () -> new ConfigNBT(null));
        registerConfigValueProvider(ConfigFluid.ID, () -> new ConfigFluid(null, null));
        registerConfigValueProvider(ConfigTeam.TEAM_ID, () -> new ConfigTeamClient(""));
    }

    private static void registerDefaultTeamActions() {
        registerTeamAction(ServerUtilitiesTeamGuiActions.CONFIG);
        registerTeamAction(ServerUtilitiesTeamGuiActions.INFO);
        registerTeamAction(ServerUtilitiesTeamGuiActions.MEMBERS);
        registerTeamAction(ServerUtilitiesTeamGuiActions.ALLIES);
        registerTeamAction(ServerUtilitiesTeamGuiActions.MODERATORS);
        registerTeamAction(ServerUtilitiesTeamGuiActions.ENEMIES);
        registerTeamAction(ServerUtilitiesTeamGuiActions.LEAVE);
        registerTeamAction(ServerUtilitiesTeamGuiActions.TRANSFER_OWNERSHIP);
    }

    private static void registerDefaultReloadHandlers() {
        registerServerReloadHandler(
                new ResourceLocation(ServerUtilities.MOD_ID, "internal_reload"),
                ServerUtilitiesCommon::onReload);
        registerServerReloadHandler(
                new ResourceLocation(ServerUtilities.MOD_ID, "ranks"),
                reloadEvent -> Ranks.INSTANCE.reload());
    }

    private static void registerDefaultSyncData() {
        registerSyncData(ServerUtilities.MOD_ID, new ServerUtilitiesSyncData());
    }

    private static void registerDefaultAdminPanelActions() {
        registerAdminPanelAction(new AdminPanelAction(ServerUtilities.MOD_ID, "reload", GuiIcons.REFRESH, -1000) {

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
        registerAdminPanelAction(
                new AdminPanelAction(ServerUtilities.MOD_ID, "crash_reports", ItemIcon.getItemIcon(Items.paper), 0) {

                    @Override
                    public Type getType(ForgePlayer player, NBTTagCompound data) {
                        return Type.fromBoolean(player.hasPermission(ServerUtilitiesPermissions.CRASH_REPORTS_VIEW));
                    }

                    @Override
                    public void onAction(ForgePlayer player, NBTTagCompound data) {
                        new MessageViewCrashList(player.team.universe.server.getFile("crash-reports"))
                                .sendTo(player.getPlayer());
                    }
                });

        registerAdminPanelAction(new AdminPanelAction(ServerUtilities.MOD_ID, "edit_world", GuiIcons.GLOBE, 0) {

            @Override
            public Type getType(ForgePlayer player, NBTTagCompound data) {
                return Type.fromBoolean(player.hasPermission(ServerUtilitiesPermissions.EDIT_WORLD_GAMERULES));
            }

            @Override
            public void onAction(ForgePlayer player, NBTTagCompound data) {
                ConfigGroup main = ConfigGroup.newGroup("edit_world");
                main.setDisplayName(new ChatComponentTranslation("serverutilities.admin_panel.edit_world"));

                if (player.hasPermission(ServerUtilitiesPermissions.EDIT_WORLD_GAMERULES)) {
                    ConfigGroup gamerules = main.getGroup("gamerules");
                    gamerules.setDisplayName(new ChatComponentTranslation("gamerules"));

                    GameRules rules = player.team.universe.world.getGameRules();

                    for (String key : rules.getRules()) {
                        String value = rules.getGameRuleStringValue(key);

                        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                            gamerules
                                    .addBool(
                                            key,
                                            () -> rules.getGameRuleBooleanValue(key),
                                            v -> rules.setOrCreateGameRule(key, Boolean.toString(v)),
                                            false)
                                    .setDisplayName(new ChatComponentText(StringUtils.camelCaseToWords(key)));
                        } else {
                            try {
                                Integer.parseInt(value);
                                gamerules
                                        .addInt(
                                                key,
                                                () -> Integer.parseInt(rules.getGameRuleStringValue(key)),
                                                v -> rules.setOrCreateGameRule(key, Integer.toString(v)),
                                                1,
                                                1,
                                                Integer.MAX_VALUE)
                                        .setDisplayName(new ChatComponentText(StringUtils.camelCaseToWords(key)));
                            } catch (NumberFormatException ignored) {
                                gamerules
                                        .addString(
                                                key,
                                                () -> rules.getGameRuleStringValue(key),
                                                v -> rules.setOrCreateGameRule(key, v),
                                                "")
                                        .setDisplayName(new ChatComponentText(StringUtils.camelCaseToWords(key)));
                            }
                        }
                    }
                }

                ServerUtilitiesAPI.editServerConfig(player.getPlayer(), main, IConfigCallback.DEFAULT);
            }

        });
        registerAdminPanelAction(
                new AdminPanelAction(ServerUtilities.MOD_ID, "edit_rank", ItemIcon.getItemIcon(Items.book), 0) {

                    @Override
                    public Type getType(ForgePlayer player, NBTTagCompound data) {
                        return Type.fromBoolean(ServerUtilitiesConfig.ranks.enabled && player.hasPermission(RANK_EDIT));
                    }

                    @Override
                    public void onAction(ForgePlayer player, NBTTagCompound data) {
                        new MessageRanks(Ranks.INSTANCE, player).sendTo(player.getPlayer());
                    }
                });
    }
}
