package serverutils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizon.gtnhlib.config.Config;

import cpw.mods.fml.common.registry.GameData;
import serverutils.data.ClaimedChunks;
import serverutils.lib.config.EnumTristate;
import serverutils.lib.item.ItemStackSerializer;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.ServerUtils;

@Config(modid = ServerUtilities.MOD_ID, category = "", configSubDirectory = "../serverutilities/")
@Config.RequiresWorldRestart
public class ServerUtilitiesConfig {

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
    public static final Pregen pregen = new Pregen();
    public static final Mixins mixins = new Mixins();
    public static final MOTD motd = new MOTD();

    public static class General {

        @Config.Comment("Merges player profiles, in case player logged in without internet connection/in offline mode server. "
                + "If set to DEFAULT, it will only merge on singleplayer worlds.")
        @Config.DefaultEnum("TRUE")
        public EnumTristate merge_offline_mode_players;

        @Config.Comment({ "Backports 1.20's 'pause-when-empty-seconds' server property", "Default value: 0 (off)" })
        @Config.DefaultBoolean(true)
        public boolean enable_pause_when_empty_property;

        @Config.Comment({
                "'max-tick-time' property introduced with Minecraft 1.8. Stops the server if a game tick is taking too long to process",
                "Default value: 0 (off)" })
        @Config.DefaultBoolean(true)
        public boolean enable_max_tick_time_property;

        @Config.Comment("Adds a button to toggle cheats to the world selection menu.")
        @Config.DefaultBoolean(true)
        public boolean enable_toggle_cheats_button;
    }

    public static class Teams {

        @Config.Comment("Disable teams entirely")
        @Config.DefaultBoolean(false)
        public boolean disable_teams;

        @Config.Comment("Automatically creates a team for player on multiplayer, based on their username and with a random color.")
        @Config.DefaultBoolean(false)
        public boolean autocreate_mp;

        @Config.Comment("Automatically creates (or joins) a team on singleplayer/LAN with ID 'singleplayer'.")
        @Config.DefaultBoolean(true)
        public boolean autocreate_sp;

        @Config.Comment("Disable no team notification entirely.")
        @Config.DefaultBoolean(false)
        public boolean hide_team_notification;

        @Config.Comment("Don't allow other players to break blocks in claimed chunks")
        @Config.DefaultBoolean(true)
        public boolean grief_protection;

        @Config.Comment("Don't allow other players to interact with blocks in claimed chunks")
        @Config.DefaultBoolean(true)
        public boolean interaction_protection;

        @Config.Comment("Forces player chat messages to be prefixed with their team name. Example: [Team] <Player> Message")
        @Config.DefaultBoolean(false)
        public boolean force_team_prefix;
    }

    public static class Debugging {

        @Config.Comment("Enables special debug commands.")
        @Config.DefaultBoolean(false)
        public boolean special_commands;

        @Config.Comment("Print more info.")
        @Config.DefaultBoolean(false)
        public boolean print_more_info;

        @Config.Comment("Print more errors.")
        @Config.DefaultBoolean(false)
        public boolean print_more_errors;

        @Config.Comment("Log incoming and outgoing network messages.")
        @Config.DefaultBoolean(false)
        public boolean log_network;

        @Config.Comment("Log player teleporting.")
        @Config.DefaultBoolean(false)
        public boolean log_teleport;

        @Config.Comment("Log config editing.")
        @Config.DefaultBoolean(false)
        public boolean log_config_editing;

        @Config.Comment("See dev-only sidebar buttons. They probably don't do anything.")
        @Config.DefaultBoolean(false)
        public boolean dev_sidebar_buttons;

        @Config.Comment("See GUI widget bounds when you hold B.")
        @Config.DefaultBoolean(false)
        public boolean gui_widget_bounds;

        @Config.Comment("Log all events that extend EventBase.")
        @Config.DefaultBoolean(false)
        public boolean log_events;

        @Config.Comment("Print a message in console every time a chunk is forced or unforced. Recommended to be off, because spam.")
        @Config.DefaultBoolean(false)
        public boolean log_chunkloading;
    }

    public static class AutoShutdown {

        @Config.Comment("Enables auto-shutdown.")
        @Config.DefaultBoolean(false)
        public boolean enabled;

        @Config.Comment("Enables auto-shutdown in singleplayer worlds.")
        @Config.DefaultBoolean(false)
        public boolean enabled_singleplayer;

        @Config.Comment("""
                Server will automatically shut down after X hours.
                Time Format: HH:MM. If the system time matches a value, server will shut down.
                It will look for closest value available that is not equal to current time.""")
        @Config.DefaultStringList({ "04:00", "16:00" })
        public String[] times;
    }

    public static class AFK {

        @Config.Comment("Enables afk timer.")
        @Config.DefaultBoolean(true)
        public boolean enabled;

        @Config.Comment("Enables afk timer in singleplayer.")
        @Config.DefaultBoolean(true)
        public boolean enabled_singleplayer;

        @Config.Comment("After how much time it will display notification to all players.")
        @Config.DefaultString("5m")
        public String notification_timer;

        @Config.Ignore
        private long notificationTimer = -1L;

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

        @Config.Comment("Adds ~ to player names that have changed nickname to prevent trolling.")
        @Config.DefaultBoolean(false)
        public boolean add_nickname_tilde;

        @Config.Comment("Replaces player names in the TAB screen with the names used in chat.")
        @Config.DefaultBoolean(true)
        public boolean replace_tab_names;
    }

    public static class Commands {

        @Config.DefaultBoolean(true)
        public boolean warp;

        @Config.DefaultBoolean(true)
        public boolean home;

        @Config.DefaultBoolean(true)
        public boolean back;

        @Config.DefaultBoolean(true)
        public boolean spawn;

        @Config.DefaultBoolean(true)
        public boolean inv;

        @Config.DefaultBoolean(true)
        public boolean tpl;

        @Config.DefaultBoolean(true)
        public boolean trash_can;

        @Config.DefaultBoolean(true)
        public boolean chunks;

        @Config.DefaultBoolean(true)
        public boolean kickme;

        @Config.DefaultBoolean(true)
        public boolean ranks;

        @Config.DefaultBoolean(true)
        public boolean heal;

        @Config.DefaultBoolean(true)
        public boolean killall;

        @Config.DefaultBoolean(true)
        public boolean nbtedit;

        @Config.DefaultBoolean(true)
        public boolean fly;

        @Config.DefaultBoolean(true)
        public boolean leaderboard;

        @Config.DefaultBoolean(true)
        public boolean tpa;

        @Config.DefaultBoolean(true)
        public boolean nick;

        @Config.DefaultBoolean(true)
        public boolean mute;

        @Config.DefaultBoolean(true)
        public boolean rtp;

        @Config.DefaultBoolean(true)
        public boolean god;

        @Config.DefaultBoolean(true)
        public boolean rec;

        @Config.DefaultBoolean(true)
        public boolean reload;

        @Config.DefaultBoolean(true)
        public boolean backup;

        @Config.DefaultBoolean(true)
        public boolean dump_chunkloaders;

        @Config.DefaultBoolean(true)
        public boolean dump_permissions;

        @Config.DefaultBoolean(true)
        public boolean dump_stats;

        @Config.DefaultBoolean(true)
        public boolean vanish;

        @Config.DefaultBoolean(true)
        public boolean seek_block;

        @Config.DefaultBoolean(true)
        public boolean pregen;
    }

    public static class Backups {

        @Config.Comment("Enables backups.")
        @Config.DefaultBoolean(true)
        public boolean enable_backups;

        @Config.Comment("Time between backups in hours. \n1.0 - backups every hour 6.0 - backups every 6 hours 0.5 - backups every 30 minutes.")
        @Config.DefaultDouble(0.5)
        @Config.RangeDouble(min = 0)
        public double backup_timer;

        @Config.Comment("Number of backup files to keep before deleting old ones.")
        @Config.DefaultInt(12)
        @Config.RangeInt(min = 1)
        public int backups_to_keep;

        @Config.Comment("How much the backup file will be compressed. 0 - uncompressed, 1 - best speed, 9 - smallest file size.")
        @Config.DefaultInt(1)
        @Config.RangeInt(min = 0, max = 9)
        public int compression_level;

        @Config.Comment("Path to backups folder.")
        @Config.DefaultString("./backups/")
        public String backup_folder_path;

        @Config.Comment("List of additional paths to include in backup. Use / as directory separator! Use * as wildcard, and $WORLDNAME for the save name. If specifying a folder, the path should end with \"/**\" to match all subfolders and files.")
        @Config.DefaultStringList({ "saves/NEI/global/**", "saves/NEI/local/$WORLDNAME/**" })
        public String[] additional_backup_files;

        @Config.Comment("Run backup in a separated thread (recommended)")
        @Config.DefaultBoolean(true)
        public boolean use_separate_thread;

        @Config.Comment("Prints (current size | total size) when backup is done")
        @Config.DefaultBoolean(true)
        public boolean display_file_size;

        @Config.Comment("Backups won't run if no players are online.")
        @Config.DefaultBoolean(true)
        public boolean need_online_players;

        @Config.Comment("Silence backup notifications.")
        @Config.DefaultBoolean(false)
        public boolean silent_backup;

        @Config.Comment("""
                Max size of backup folder in GB. If total folder size exceeds this value it will delete old backups until the size is under.
                0 = Disabled and backups_to_keep will be used instead.""")
        @Config.DefaultInt(0)
        @Config.RangeInt(min = 0)
        public int max_folder_size;

        @Config.Comment("Delete backups that have a custom name set through /backup start <name>")
        @Config.DefaultBoolean(true)
        public boolean delete_custom_name_backups;

        @Config.Comment("""
                Only include claimed chunks in backup.
                Backups will be much faster and smaller, but any unclaimed chunk will be unrecoverable.""")
        @Config.DefaultBoolean(false)
        public boolean only_backup_claimed_chunks;

        @Config.Comment("""
                Backup entire regions that contain at least one claimed chunk.
                This backs up complete .mca files instead of reconstructing temporary files with only claimed chunks.
                Requires only_backup_claimed_chunks to be enabled.""")
        @Config.DefaultBoolean(false)
        public boolean backup_entire_regions_with_claims;
    }

    public static class Login {

        @Config.Comment("Enables message of the day.")
        @Config.DefaultBoolean(false)
        public boolean enable_motd;

        @Config.Comment("Enables starting items.")
        @Config.DefaultBoolean(false)
        public boolean enable_starting_items;

        @Config.Comment("Message of the day. This will be displayed when player joins the server.")
        @Config.DefaultStringList("Hello player!")
        public String[] motd;

        @Config.Comment("Items to give player when they first join the server.\nFormat: '{id:\"ID\",Count:X,Damage:X,tag:{}}', Use /print_item to get NBT of item in your hand.")
        @Config.DefaultStringList({
                "{id:\"minecraft:stone_sword\",Count:1,Damage:1,tag:{display:{Name:\"Epic Stone Sword\"}}}" })
        public String[] starting_items;

        @Config.Ignore
        private List<IChatComponent> motdComponents = null;

        @Config.Ignore
        private List<ItemStack> startingItems = null;

        public List<IChatComponent> getMOTD() {
            if (motdComponents == null) {
                motdComponents = new ArrayList<>();

                if (enable_motd) {
                    for (String s : motd) {
                        motdComponents.add(ForgeHooks.newChatWithLinks(s));
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
                            ServerUtilities.LOGGER.warn("Failed to parse starting item: {}", s, ex);
                        }
                    }
                }
            }

            return startingItems;
        }
    }

    public static class RanksConfig {

        @Config.Comment("Enables Ranks.")
        @Config.DefaultBoolean(true)
        public boolean enabled;

        @Config.Comment("Adds chat colors/rank-specific syntax.")
        @Config.DefaultBoolean(true)
        public boolean override_chat;

        @Config.Comment("Add permissions for commands and allow them to be controlled by ranks.")
        @Config.DefaultBoolean(true)
        public boolean command_permissions;
    }

    public static class WorldConfig {

        public static class WorldLogging {

            @Config.Comment("Enables world logging.")
            @Config.DefaultBoolean(false)
            public boolean enabled;

            @Config.Comment("Includes creative players in world logging.")
            @Config.DefaultBoolean(false)
            public boolean include_creative_players;

            @Config.Comment("Includes fake players in world logging.")
            @Config.DefaultBoolean(false)
            public boolean include_fake_players;

            @Config.Comment("Logs block placement.")
            @Config.DefaultBoolean(true)
            public boolean block_placed;

            @Config.Comment("Logs block breaking.")
            @Config.DefaultBoolean(true)
            public boolean block_broken;

            @Config.Comment("Logs item clicking in air.")
            @Config.DefaultBoolean(true)
            public boolean item_clicked_in_air;

            @Config.Comment("Logs player attacks on other players/entites.")
            @Config.DefaultBoolean(true)
            public boolean entity_attacked;

            @Config.Comment("Exclude mobs from entity attack logging.")
            @Config.DefaultBoolean(true)
            public boolean exclude_mob_entity;

            @Config.Comment("Enables chat logging.")
            @Config.DefaultBoolean(false)
            public boolean chat_enable;

            public boolean log(EntityPlayerMP player) {
                return enabled && (include_creative_players || !player.capabilities.isCreativeMode)
                        && (include_fake_players || !ServerUtils.isFake(player));
            }
        }

        public final WorldLogging logging = new WorldLogging();

        @Config.Comment("Enables chunk claiming.")
        @Config.DefaultBoolean(true)
        public boolean chunk_claiming;

        @Config.Comment("Enables chunk loading. If chunk_claiming is set to false, changing this won't do anything.")
        @Config.DefaultBoolean(true)
        public boolean chunk_loading;

        @Config.Comment("If set to true, explosions and hostile mobs in spawn area will be disabled, players won't be able to attack each other in spawn area.")
        @Config.DefaultBoolean(false)
        public boolean safe_spawn;

        @Config.Comment("Enable spawn area in singleplayer.")
        @Config.DefaultBoolean(false)
        public boolean spawn_area_in_sp;

        @Config.Comment("Dimensions where chunk claiming isn't allowed.")
        @Config.DefaultIntList({})
        public int[] blocked_claiming_dimensions;

        @Config.Comment("""
                Allowed values:
                DEFAULT = Players can choose their own PVP status.
                TRUE = PVP on for everyone.
                FALSE = PVP disabled for everyone.""")
        @Config.DefaultEnum("DEFAULT")
        public EnumTristate enable_pvp;

        @Config.Comment("""
                Allowed values:
                DEFAULT = Teams can decide their explosion setting
                TRUE = Explosions on for everyone.
                FALSE = Explosions disabled for everyone.""")
        @Config.DefaultEnum("DEFAULT")
        public EnumTristate enable_explosions;

        @Config.Comment("""
                Requires chunk_claiming and mixins:endermen to be true.
                Allowed values:
                DEFAULT = Teams can decide their enderman setting
                TRUE = Enderman block interactions on for everyone.
                FALSE = Enderman block interactions disabled for everyone.""")
        @Config.DefaultEnum("DEFAULT")
        public EnumTristate enable_endermen;

        @Config.Comment("Spawn radius. You must set spawn-protection in server.properties file to 0!")
        @Config.DefaultInt(0)
        public int spawn_radius;

        @Config.Comment("Spawn dimension. Overworld by default.")
        @Config.DefaultInt(0)
        public int spawn_dimension;

        @Config.Comment("Unloads erroring chunks if dimension isn't loaded or some other problem occurs.")
        @Config.DefaultBoolean(false)
        public boolean unload_erroring_chunks;

        @Config.Comment("Min /rtp distance")
        @Config.DefaultDouble(1000D)
        public double rtp_min_distance;

        @Config.Comment("Max /rtp distance")
        @Config.DefaultDouble(100000D)
        public double rtp_max_distance;

        @Config.Comment("Max tries /rtp does before failure.")
        @Config.DefaultInt(200)
        public int rtp_max_tries;

        @Config.Comment("""
                List of items that will have right-click function disabled on both sides.
                You can use '/inv disable_right_click' command to do with from in-game.
                Syntax: modid:item:metadata. Set metadata to * to ignore it.""")
        @Config.DefaultStringList({ "" })
        public String[] disabled_right_click_items;

        @Config.Comment("""
                Locked time in ticks in spawn dimension.
                -1 - Disabled
                0 - Morning
                6000 - Noon
                12000 - Evening
                18000 - Midnight""")
        @Config.RangeInt(min = -1, max = 23999)
        @Config.DefaultInt(-1)
        public int forced_spawn_dimension_time;

        @Config.Comment("""
                Locked weather type in spawn dimension.
                -1 - Disabled
                0 - Clear
                1 - Raining
                2 - Thunderstorm""")
        @Config.RangeInt(min = -1, max = 2)
        @Config.DefaultInt(-1)
        public int forced_spawn_dimension_weather;

        @Config.Comment("Disables player damage when they are stuck in walls.")
        @Config.DefaultBoolean(false)
        public boolean disable_player_suffocation_damage;

        @Config.Comment("Show play time in corner.")
        @Config.DefaultBoolean(false)
        public boolean show_playtime;

        @Config.Comment("Enabled Player Sleeping Percentage to skip night. Use the gamerule playersSleepingPercentage to set the percentage.")
        @Config.DefaultBoolean(true)
        @Config.ModDetectedDefault(coremod = "ganymedes01.etfuturum.mixinplugin.EtFuturumEarlyMixins", value = "false")
        public boolean enable_player_sleeping_percentage;

        @Config.Comment("Default Player Sleeping. This is only what the gamerule is initially set to, not the active value that is used.")
        @Config.DefaultInt(50)
        @Config.RangeInt(min = 0, max = 100)
        public int player_sleeping_percentage;

        @Config.Ignore
        private List<DisabledItem> disabledItems = null;

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

            @Config.Comment("Enables periodic removal of entities")
            @Config.DefaultBoolean(false)
            public boolean enabled;

            @Config.Comment("How often the cleanup should run in hours")
            @Config.DefaultDouble(2)
            public double interval;

            @Config.Comment("Include hostile mobs in cleanup")
            @Config.DefaultBoolean(true)
            public boolean hostiles;

            @Config.Comment("Include passive mobs in cleanup")
            @Config.DefaultBoolean(false)
            public boolean passives;

            @Config.Comment("Include items on the ground in cleanup")
            @Config.DefaultBoolean(true)
            public boolean items;

            @Config.Comment("Include experience orbs in cleanup")
            @Config.DefaultBoolean(true)
            public boolean experience;

            @Config.Comment("Silence cleanup warning that are sent prior to starting")
            @Config.DefaultBoolean(false)
            public boolean silent;
        }

        public final Cleanup cleanup = new Cleanup();
    }

    public static class Pregen {

        @Config.Comment("When pregeneration is active, queue this many chunks per tick.")
        @Config.DefaultInt(1)
        public int chunksPerTick;

        @Config.Comment("The maximum time to spend on pregeneration per tick, in milliseconds. Note that chunk unloading also takes time, and isn't limited by this config!")
        @Config.DefaultFloat(25)
        public float timeLimitMs;

        public long timeLimitNanos() {
            return (long) (timeLimitMs * 1_000_000);
        }
    }

    @Config.RequiresMcRestart
    public static class Mixins {

        @Config.Comment("""
                Enable the mixins that control enderman behavior. This is required by world:enable_endermen.
                Allowed values:
                TRUE = Enable the enderman mixins.
                FALSE = Disable the enderman mixins.""")
        @Config.DefaultBoolean(true)
        public boolean endermen;

        @Config.Comment("Adds a permission node (serverutilities.bypass_player_limit) that allows for joining while server is full.")
        @Config.DefaultBoolean(true)
        public boolean bypassPlayerLimit;
    }

    public static class MOTD {

        @Config.Comment("Enable custom configurable SERVER MOTD with color codes and variables")
        @Config.DefaultBoolean(false)
        public boolean enabled;

        @Config.Comment("First line of MOTD. Supports color codes (§), variables ({players}, {maxPlayers}, {tps}, {memory}, {uptime})")
        @Config.DefaultString("§6§lMy Minecraft Server")
        public String line1;

        @Config.Comment("Second line of MOTD. Supports color codes (§), variables ({players}, {maxPlayers}, {tps}, {memory}, {uptime})")
        @Config.DefaultString("§aUptime: §f{uptime} §7| §bTPS: §f{tps}")
        public String line2;
    }
}
