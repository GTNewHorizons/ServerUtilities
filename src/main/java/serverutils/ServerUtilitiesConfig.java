package serverutils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameData;
import serverutils.data.ClaimedChunks;
import serverutils.lib.config.EnumTristate;
import serverutils.lib.io.DataReader;
import serverutils.lib.item.ItemStackSerializer;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.JsonUtils;
import serverutils.lib.util.ServerUtils;

public class ServerUtilitiesConfig {

    public static Configuration config;
    public static final String GEN_CAT = Configuration.CATEGORY_GENERAL;
    public static final String TEAM_CAT = "team";
    public static final String DEBUG_CAT = "debugging";
    public static final String AUTO_SHUTDOWN = "auto_shutdown";
    public static final String AFK = "afk";
    public static final String CHAT = "chat";
    public static final String COMMANDS = "commands";
    public static final String LOGIN = "login";
    public static final String RANKS = "ranks";
    public static final String WORLD = "world";
    public static final String LOGGING = WORLD + ".logging";
    public static final String DEBUGGING = "debugging";
    public static final String BACKUPS = "backups";
    public static final String TASKS = "tasks";
    public static final String TASK_CLEANUP = TASKS + ".cleanup";

    public static final String[] TRISTATE_VALUES = { "TRUE", "FALSE", "DEFAULT" };

    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(
                new File(event.getModConfigurationDirectory() + "/../serverutilities/serverutilities.cfg"));
        config.load();
        sync();
    }

    public static boolean sync() {
        general.replace_reload_command = config.get(
                GEN_CAT,
                "replace_reload_command",
                true,
                "This will replace /reload with ServerUtilities version of it.").getBoolean();
        general.merge_offline_mode_players = EnumTristate.string2tristate(
                config.get(
                        GEN_CAT,
                        "merge_offline_mode_players",
                        EnumTristate.TRUE.getName(),
                        "Merges player profiles, in case player logged in without internet connection/in offline mode server. If set to DEFAULT, it will only merge on singleplayer worlds.")
                        .getString());

        config.setCategoryRequiresWorldRestart(GEN_CAT, true);

        teams.disable_teams = config.get(TEAM_CAT, "disable_teams", false, "Disable teams entirely").getBoolean();
        teams.autocreate_mp = config.get(
                TEAM_CAT,
                "autocreate_mp",
                false,
                "Automatically creates a team for player on multiplayer, based on their username and with a random color.")
                .getBoolean();
        teams.autocreate_sp = config
                .get(
                        TEAM_CAT,
                        "autocreate_sp",
                        true,
                        "Automatically creates (or joins) a team on singleplayer/LAN with ID 'singleplayer'.")
                .getBoolean();
        teams.hide_team_notification = config
                .get(TEAM_CAT, "hide_team_notification", false, "Disable no team notification entirely.")
                .setLanguageKey("player_config.serverutilities.hide_team_notification").getBoolean();

        teams.grief_protection = config
                .get(TEAM_CAT, "grief_protection", true, "Don't allow other players to break blocks in claimed chunks")
                .getBoolean();
        teams.interaction_protection = config.get(
                TEAM_CAT,
                "interaction_protection",
                true,
                "Don't allow other players to interact with blocks in claimed chunks").getBoolean();
        teams.force_team_prefix = config.get(
                TEAM_CAT,
                "force_team_prefix",
                false,
                "Forces player chat messages to be prefixed with their team name. Example: [Team] <Player> Message")
                .getBoolean();

        config.setCategoryComment(DEBUG_CAT, "Don't set any values to true, unless you are debugging the mod.");
        debugging.special_commands = config.get(DEBUG_CAT, "special_commands", false, "Enables special debug commands.")
                .getBoolean();
        debugging.print_more_info = config.get(DEBUG_CAT, "print_more_info", false, "Print more info.").getBoolean();
        debugging.print_more_errors = config.get(DEBUG_CAT, "print_more_errors", false, "Print more errors.")
                .getBoolean();
        debugging.log_network = config
                .get(DEBUG_CAT, "log_network", false, "Log incoming and outgoing network messages.").getBoolean();
        debugging.log_teleport = config.get(DEBUG_CAT, "log_teleport", false, "Log player teleporting.").getBoolean();
        debugging.log_config_editing = config.get(DEBUG_CAT, "log_config_editing", false, "Log config editing.")
                .getBoolean();
        debugging.dev_sidebar_buttons = config.get(
                DEBUG_CAT,
                "dev_sidebar_buttons",
                false,
                "See dev-only sidebar buttons. They probably don't do anything.").getBoolean();
        debugging.gui_widget_bounds = config
                .get(DEBUG_CAT, "gui_widget_bounds", false, "See GUI widget bounds when you hold B.").getBoolean();
        debugging.log_events = config.get(DEBUG_CAT, "log_events", false, "Log all events that extend EventBase.")
                .getBoolean();
        debugging.log_chunkloading = config.get(
                DEBUGGING,
                "log_chunkloading",
                false,
                "Print a message in console every time a chunk is forced or unforced. Recommended to be off, because spam.")
                .getBoolean();

        config.setCategoryRequiresWorldRestart(AUTO_SHUTDOWN, true);
        auto_shutdown.enabled = config.get(AUTO_SHUTDOWN, "enabled", false, "Enables auto-shutdown.").getBoolean();
        auto_shutdown.enabled_singleplayer = config
                .get(AUTO_SHUTDOWN, "enabled_singleplayer", false, "Enables auto-shutdown in singleplayer worlds.")
                .getBoolean();
        auto_shutdown.times = config.get(
                AUTO_SHUTDOWN,
                "times",
                new String[] { "04:00", "16:00" },
                "Server will automatically shut down after X hours.\nTime Format: HH:MM. If the system time matches a value, server will shut down.\nIt will look for closest value available that is not equal to current time.")
                .getStringList();

        afk.enabled = config.get(AFK, "enabled", true, "Enables afk timer.").getBoolean();
        afk.enabled_singleplayer = config.get(AFK, "enabled_singleplayer", false, "Enables afk timer in singleplayer.")
                .getBoolean();
        afk.notification_timer = config
                .get(AFK, "notificationTimer", "5m", "After how much time it will display notification to all players.")
                .getString();
        afk.log_afk = config.get(AFK, "log_afk", false, "Will print in console when someone goes/comes back from AFK.")
                .getBoolean();

        backups.enable_backups = config.get(BACKUPS, "enable_backups", true, "Enables backups.").getBoolean();
        backups.compression_level = config.get(
                BACKUPS,
                "compression_level",
                1,
                "How much the backup file will be compressed. 1 - best speed 9 - smallest file size.").getInt();
        backups.backups_to_keep = config
                .get(BACKUPS, "backups_to_keep", 12, "Number of backup files to keep before deleting old ones.")
                .getInt();
        backups.backup_folder_path = config.get(BACKUPS, "backup_folder_path", "./backups/", "Path to backups folder.")
                .getString();
        backups.backup_timer = config.get(
                BACKUPS,
                "backup_timer",
                "0.5",
                "Time between backups in hours. \n1.0 - backups every hour 6.0 - backups every 6 hours 0.5 - backups every 30 minutes.")
                .getDouble();
        backups.display_file_size = config
                .get(BACKUPS, "display_file_size", true, "Prints (current size | total size) when backup is done")
                .getBoolean();
        backups.silent_backup = config.get(BACKUPS, "silent_backup", false, "Silence backup notifications.")
                .getBoolean();
        backups.use_separate_thread = config
                .get(BACKUPS, "use_separate_thread", true, "Run backup in a separated thread (recommended)")
                .getBoolean();
        backups.need_online_players = config
                .get(BACKUPS, "need_online_players", true, "Backups won't run if no players are online.").getBoolean();

        chat.add_nickname_tilde = config.get(
                CHAT,
                "add_nickname_tilde",
                false,
                "Adds ~ to player names that have changed nickname to prevent trolling.").getBoolean();

        commands.warp = config.get(COMMANDS, "warp", true).getBoolean();
        commands.home = config.get(COMMANDS, "home", true).getBoolean();
        commands.back = config.get(COMMANDS, "back", true).getBoolean();
        commands.spawn = config.get(COMMANDS, "spawn", true).getBoolean();
        commands.inv = config.get(COMMANDS, "inv", true).getBoolean();
        commands.tpl = config.get(COMMANDS, "tpl", true).getBoolean();
        commands.trash_can = config.get(COMMANDS, "trash_can", true).getBoolean();
        commands.chunks = config.get(COMMANDS, "chunks", true).getBoolean();
        commands.kickme = config.get(COMMANDS, "kickme", true).getBoolean();
        commands.ranks = config.get(COMMANDS, "ranks", true).getBoolean();
        commands.heal = config.get(COMMANDS, "heal", true).getBoolean();
        commands.killall = config.get(COMMANDS, "killall", true).getBoolean();
        commands.nbtedit = config.get(COMMANDS, "nbtedit", true).getBoolean();
        commands.fly = config.get(COMMANDS, "fly", true).getBoolean();
        commands.leaderboard = config.get(COMMANDS, "leaderboard", true).getBoolean();
        commands.tpa = config.get(COMMANDS, "tpa", true).getBoolean();
        commands.nick = config.get(COMMANDS, "nick", true).getBoolean();
        commands.mute = config.get(COMMANDS, "mute", true).getBoolean();
        commands.rtp = config.get(COMMANDS, "rtp", true).getBoolean();
        commands.god = config.get(COMMANDS, "god", true).getBoolean();
        commands.rec = config.get(COMMANDS, "rec", true).getBoolean();
        commands.backup = config.get(COMMANDS, "backup", true).getBoolean();
        commands.dump_chunkloaders = config.get(COMMANDS, "dump_chunkloaders", true).getBoolean();
        commands.dump_permissions = config.get(COMMANDS, "dump_permissions", true).getBoolean();

        login.enable_motd = config.get(LOGIN, "enable_motd", false, "Enables message of the day.").getBoolean();
        login.enable_starting_items = config.get(LOGIN, "enable_starting_items", false, "Enables starting items.")
                .getBoolean();
        login.motd = config.get(
                LOGIN,
                "motd",
                new String[] { "\"Hello player!\"" },
                "Message of the day. This will be displayed when player joins the server.").getStringList();
        login.starting_items = config.get(
                LOGIN,
                "starting_items",
                new String[] {
                        "{id:\"minecraft:stone_sword\",Count:1,Damage:1,tag:{display:{Name:\"Epic Stone Sword\"}}}" },
                "Items to give player when they first join the server.\nFormat: '{id:\"ID\",Count:X,Damage:X,tag:{}}', Use /print_item to get NBT of item in your hand.")
                .getStringList();

        ranks.enabled = config.get(
                RANKS,
                "enabled",
                true,
                "Enables ranks and adds command.x permissions and allows ranks to control them.").getBoolean();
        ranks.override_chat = config.get(RANKS, "override_chat", true, "Adds chat colors/rank-specific syntax.")
                .getBoolean();
        ranks.override_commands = config.get(
                RANKS,
                "override_commands",
                true,
                "Allow to configure commands with ranks. Disable this if you want to use other permission mod for that.")
                .getBoolean();
        config.setCategoryRequiresMcRestart(RANKS, true);

        world.logging.enabled = config.get(LOGGING, "enabled", false, "Enables world logging.").getBoolean();
        world.logging.include_creative_players = config
                .get(LOGGING, "include_creative_players", false, "Includes creative players in world logging.")
                .getBoolean();
        world.logging.include_fake_players = config
                .get(LOGGING, "include_fake_players", false, "Includes fake players in world logging.").getBoolean();
        world.logging.block_placed = config.get(LOGGING, "block_placed", true, "Logs block placement.").getBoolean();
        world.logging.block_broken = config.get(LOGGING, "block_broken", true, "Logs block breaking.").getBoolean();
        world.logging.item_clicked_in_air = config
                .get(LOGGING, "item_clicked_in_air", true, "Logs item clicking in air.").getBoolean();
        world.logging.entity_attacked = config
                .get(LOGGING, "entity_attacked", true, "Logs player attacks on other players/entites.").getBoolean();
        world.logging.exclude_mob_entity = config
                .get(LOGGING, "exclude_mob_entity", true, "Exclude mobs from entity attack logging.").getBoolean();
        world.logging.chat_enable = config.get(LOGGING, "chat_enable", false, "Enables chat logging.").getBoolean();
        config.setCategoryComment(LOGGING, "Logs different events in logs/world.log file.");

        world.chunk_claiming = config.get(WORLD, "chunk_claiming", true, "Enables chunk claiming.").getBoolean();
        world.chunk_loading = config
                .get(
                        WORLD,
                        "chunk_loading",
                        true,
                        "Enables chunk loading. If chunk_claiming is set to false, changing this won't do anything.")
                .getBoolean();
        world.safe_spawn = config.get(
                WORLD,
                "safe_spawn",
                false,
                "If set to true, explosions and hostile mobs in spawn area will be disabled, players won't be able to attack each other in spawn area.")
                .getBoolean();
        world.spawn_area_in_sp = config.get(WORLD, "spawn_area_in_sp", false, "Enable spawn area in singleplayer.")
                .getBoolean();
        world.blocked_claiming_dimensions = config.get(
                WORLD,
                "blocked_claiming_dimensions",
                new int[] {},
                "Dimensions where chunk claiming isn't allowed.").getIntList();
        world.enable_pvp = EnumTristate.string2tristate(config.get(WORLD, "enable_pvp", EnumTristate.TRUE.getName(), """
                Allowed values:
                DEFAULT = Players can choose their own PVP status.
                TRUE = PVP on for everyone.
                FALSE = PVP disabled for everyone.""").setValidValues(TRISTATE_VALUES).getString());
        world.enable_explosions = EnumTristate
                .string2tristate(config.get(WORLD, "enable_explosions", EnumTristate.DEFAULT.getName(), """
                        Allowed values:
                        DEFAULT = Teams can decide their explosion setting
                        TRUE = Explosions on for everyone.
                        FALSE = Explosions disabled for everyone.""").setValidValues(TRISTATE_VALUES).getString());
        world.spawn_radius = config.get(
                WORLD,
                "spawn_radius",
                0,
                "Spawn radius. You must set spawn-protection in server.properties file to 0!").getInt();
        world.spawn_dimension = config.get(WORLD, "spawn_dimension", 0, "Spawn dimension. Overworld by default.")
                .getInt();
        world.unload_erroring_chunks = config.get(
                WORLD,
                "unload_erroring_chunks",
                false,
                "Unloads erroring chunks if dimension isn't loaded or some other problem occurs.").getBoolean();
        world.rtp_min_distance = config.get(WORLD, "rtp_min_distance", 1000D, "Min /rtp distance").getDouble();
        world.rtp_max_distance = config.get(WORLD, "rtp_max_distance", 100000D, "Max /rtp distance").getDouble();
        world.rtp_max_tries = config.get(WORLD, "rtp_max_tries", 200, "Max tries /rtp does before failure.").getInt();
        world.disabled_right_click_items = config.get(
                WORLD,
                "disabled_right_click_items",
                new String[] {},
                "List of items that will have right-click function disabled on both sides.\nYou can use '/inv disable_right_click' command to do with from in-game.\nSyntax: modid:item:metadata. Set metadata to * to ignore it.")
                .getStringList();
        world.forced_spawn_dimension_time = config.get(
                WORLD,
                "forced_spawn_dimension_time",
                -1,
                "Locked time in ticks in spawn dimension.\n-1 - Disabled\n0 - Morning\n6000 - Noon\n12000 - Evening\n18000 - Midnight",
                -1,
                23999).getInt();
        world.forced_spawn_dimension_weather = config.get(
                WORLD,
                "forced_spawn_dimension_weather",
                -1,
                "Locked weather type in spawn dimension.\n-1 - Disabled\n0 - Clear\n1 - Raining\n2 - Thunderstorm",
                -1,
                2).getInt();
        world.disable_player_suffocation_damage = config.get(
                WORLD,
                "disable_player_suffocation_damage",
                false,
                "Disables player damage when they are stuck in walls.").getBoolean();
        world.show_playtime = config.get(WORLD, "show_playtime", false, "Show play time in corner.").getBoolean();
        config.setCategoryRequiresWorldRestart(WORLD, true);

        tasks.cleanup.enabled = config
                .get(TASK_CLEANUP, "cleanup_enabled", false, "Enables periodic removal of entities").getBoolean();
        tasks.cleanup.interval = config
                .get(TASK_CLEANUP, "cleanup_interval", 2.0, "How often the cleanup should run in hours").getDouble();
        tasks.cleanup.hostiles = config.get(TASK_CLEANUP, "include_hostiles", true, "Include hostile mobs in cleanup")
                .getBoolean();
        tasks.cleanup.passives = config.get(TASK_CLEANUP, "include_passives", false, "Include passive mobs in cleanup")
                .getBoolean();
        tasks.cleanup.items = config.get(TASK_CLEANUP, "include_items", true, "Include items on the ground in cleanup")
                .getBoolean();
        tasks.cleanup.experience = config
                .get(TASK_CLEANUP, "include_experience", true, "Include experience orbs in cleanup").getBoolean();
        tasks.cleanup.silent = config
                .get(TASK_CLEANUP, "silent_cleanup", false, "Silence cleanup warning that are sent prior to starting")
                .getBoolean();

        login.motdComponents = null;
        login.startingItems = null;
        afk.notificationTimer = -1L;
        world.disabledItems = null;

        config.save();

        return true;
    }

    public static final AutoShutdown auto_shutdown = new AutoShutdown();
    public static final AFK afk = new AFK();
    public static final Chat chat = new Chat();
    public static final Commands commands = new Commands();
    public static final Login login = new Login();
    public static final RanksConfig ranks = new RanksConfig();
    public static final WorldConfig world = new WorldConfig();
    public static final Debugging debugging = new Debugging();
    public static final Backups backups = new Backups();
    public static final General general = new General();
    public static final Teams teams = new Teams();
    public static final Tasks tasks = new Tasks();

    public static class General {

        public boolean replace_reload_command;
        public EnumTristate merge_offline_mode_players;
    }

    public static class Teams {

        public boolean disable_teams;
        public boolean autocreate_mp;
        public boolean autocreate_sp;
        public boolean hide_team_notification;
        public boolean grief_protection;
        public boolean interaction_protection;
        public boolean force_team_prefix;
    }

    public static class Debugging {

        public boolean special_commands;
        public boolean print_more_info;
        public boolean print_more_errors;
        public boolean log_network;
        public boolean log_teleport;
        public boolean log_config_editing;
        public boolean dev_sidebar_buttons;
        public boolean gui_widget_bounds;
        public boolean log_events;
        public boolean log_chunkloading;
    }

    public static class AutoShutdown {

        public boolean enabled;
        public boolean enabled_singleplayer;
        public String[] times;
    }

    public static class AFK {

        public boolean enabled;
        public boolean enabled_singleplayer;
        public String notification_timer;
        public boolean log_afk;
        private long notificationTimer;

        public boolean isEnabled(MinecraftServer server) {
            return enabled && (enabled_singleplayer || !server.isSinglePlayer());
        }

        public long getNotificationTimer() {
            if (notificationTimer < 0L) {
                notificationTimer = Ticks.get(notification_timer).millis();
            }

            return notificationTimer;
        }
    }

    public static class Chat {

        public boolean add_nickname_tilde;
        public boolean replace_tab_names;
    }

    public static class Commands {

        public boolean warp;
        public boolean home;
        public boolean back;
        public boolean spawn;
        public boolean inv;
        public boolean tpl;
        public boolean trash_can;
        public boolean chunks;
        public boolean kickme;
        public boolean ranks;
        public boolean heal;
        public boolean killall;
        public boolean nbtedit;
        public boolean fly;
        public boolean leaderboard;
        public boolean tpa;
        public boolean nick;
        public boolean mute;
        public boolean rtp;
        public boolean god;
        public boolean rec;
        public boolean backup;
        public boolean dump_chunkloaders;
        public boolean dump_permissions;
    }

    public static class Backups {

        public boolean enable_backups;
        public double backup_timer;
        public int backups_to_keep;
        public int compression_level;
        public String backup_folder_path;
        public boolean use_separate_thread;
        public boolean display_file_size;
        public boolean need_online_players;
        public boolean silent_backup;
    }

    public static class Login {

        public boolean enable_motd;
        public boolean enable_starting_items;
        public String[] motd;
        private List<IChatComponent> motdComponents = null;
        private List<ItemStack> startingItems = null;
        public String[] starting_items;

        public List<IChatComponent> getMOTD() {
            if (motdComponents == null) {
                motdComponents = new ArrayList<>();

                if (enable_motd) {
                    for (String s : motd) {
                        IChatComponent t = JsonUtils.deserializeTextComponent(DataReader.get(s).safeJson());

                        if (t != null) {
                            motdComponents.add(t);
                        }
                    }
                }
            }

            return motdComponents;
        }

        public List<ItemStack> getStartingItems() {
            if (startingItems == null) {
                startingItems = new ArrayList<>();

                if (enable_starting_items) {
                    for (String s : starting_items) {
                        try {
                            ItemStack stack = ItemStackSerializer.parseItem(s);

                            if (stack != null) {
                                startingItems.add(stack);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

            return startingItems;
        }
    }

    public static class RanksConfig {

        public boolean enabled;
        public boolean override_chat;
        public boolean override_commands;
    }

    public static class WorldConfig {

        public static class WorldLogging {

            public boolean enabled;
            public boolean include_creative_players;
            public boolean include_fake_players;
            public boolean block_placed;
            public boolean block_broken;
            public boolean item_clicked_in_air;
            public boolean entity_attacked;
            public boolean exclude_mob_entity;
            public boolean chat_enable;

            public boolean log(EntityPlayerMP player) {
                return enabled && (include_creative_players || !player.capabilities.isCreativeMode)
                        && (include_fake_players || !ServerUtils.isFake(player));
            }
        }

        public final WorldLogging logging = new WorldLogging();

        public boolean chunk_claiming;
        public boolean chunk_loading;
        public boolean safe_spawn;
        public boolean spawn_area_in_sp;
        public int[] blocked_claiming_dimensions;
        public EnumTristate enable_pvp;
        public EnumTristate enable_explosions;
        public int spawn_radius;
        public int spawn_dimension;
        public boolean unload_erroring_chunks;
        public double rtp_min_distance;
        public double rtp_max_distance;
        public int rtp_max_tries;
        public String[] disabled_right_click_items;
        private List<DisabledItem> disabledItems = null;
        public int forced_spawn_dimension_time;
        public int forced_spawn_dimension_weather;
        public boolean disable_player_suffocation_damage;
        public boolean show_playtime;

        private static class DisabledItem {

            private Item item;
            private int metadata;
        }

        public boolean blockDimension(int dimension) {
            if (!ClaimedChunks.isActive()) {
                return true;
            }

            for (int i : blocked_claiming_dimensions) {
                if (i == dimension) {
                    return true;
                }
            }

            return false;
        }

        public boolean isItemRightClickDisabled(ItemStack stack) {
            if (disabledItems == null) {
                disabledItems = new ArrayList<>();

                for (String s : disabled_right_click_items) {
                    String[] s1 = s.split("@", 2);
                    Item item = GameData.getItemRegistry().getObject(s1[0]);

                    if (item != null) {
                        DisabledItem di = new DisabledItem();
                        di.item = item;
                        di.metadata = (s1.length == 1 || s1[1].startsWith("*")) ? OreDictionary.WILDCARD_VALUE
                                : Integer.parseInt(s1[1].trim());
                        disabledItems.add(di);
                    }
                }
            }

            if (disabledItems.isEmpty()) {
                return false;
            }

            Item item = stack.getItem();
            int meta = stack.getItemDamage();

            for (DisabledItem disabledItem : disabledItems) {
                if (disabledItem.item == item
                        && (disabledItem.metadata == OreDictionary.WILDCARD_VALUE || disabledItem.metadata == meta)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static class Tasks {

        public static class Cleanup {

            public boolean enabled;
            public double interval;
            public boolean hostiles;
            public boolean passives;
            public boolean items;
            public boolean experience;
            public boolean silent;
        }

        public final Cleanup cleanup = new Cleanup();
    }
}
