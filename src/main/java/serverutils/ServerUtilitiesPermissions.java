package serverutils;

import java.util.HashSet;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucket;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameData;
import serverutils.data.Leaderboard;
import serverutils.events.CustomPermissionPrefixesRegistryEvent;
import serverutils.events.RegisterRankConfigEvent;
import serverutils.events.RegisterRankConfigHandlerEvent;
import serverutils.lib.config.ConfigBoolean;
import serverutils.lib.config.ConfigEnum;
import serverutils.lib.config.ConfigInt;
import serverutils.lib.config.ConfigString;
import serverutils.lib.config.ConfigTimer;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.permission.DefaultPermissionLevel;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.lib.util.text_components.TextComponentParser;
import serverutils.ranks.Rank;
import serverutils.ranks.ServerUtilitiesPermissionHandler;

public class ServerUtilitiesPermissions {

    public static final ServerUtilitiesPermissions INST = new ServerUtilitiesPermissions();
    // Display //
    public static final String DISPLAY_ADMIN_INFO = "serverutilities.display.admin_info";

    // Homes //
    public static final String HOMES_CROSS_DIM = "serverutilities.homes.cross_dim";
    public static final String HOMES_MAX = "serverutilities.homes.max";
    public static final String HOMES_COOLDOWN = "serverutilities.homes.cooldown";
    public static final String HOMES_WARMUP = "serverutilities.homes.warmup";
    public static final String HOMES_LIST_OTHER = "serverutilities.other_player.homes.list";
    public static final String HOMES_TELEPORT_OTHER = "serverutilities.other_player.homes.teleport";

    // Warps //
    public static final String WARPS_COOLDOWN = "serverutilities.warps.cooldown";
    public static final String WARPS_WARMUP = "serverutilities.warps.warmup";

    public static final String HOMES_BACK = "serverutilities.back.home";
    public static final String WARPS_BACK = "serverutilities.back.warp";
    public static final String SPAWN_BACK = "serverutilities.back.spawn";
    public static final String TPA_BACK = "serverutilities.back.tpa";
    public static final String RTP_BACK = "serverutilities.back.rtp";
    public static final String RESPAWN_BACK = "serverutilities.back.respawn";
    public static final String BACK_BACK = "serverutilities.back.back";

    // Claims //
    public static final String CLAIMS_OTHER_SEE_INFO = "serverutilities.other_player.claims.see_info";
    public static final String CLAIMS_OTHER_CLAIM = "serverutilities.other_player.claims.claim";
    public static final String CLAIMS_OTHER_UNCLAIM = "serverutilities.other_player.claims.unclaim";
    public static final String CLAIMS_OTHER_LOAD = "serverutilities.other_player.claims.load";
    public static final String CLAIMS_OTHER_UNLOAD = "serverutilities.other_player.claims.unload";
    public static final String CLAIMS_MAX_CHUNKS = "serverutilities.claims.max_chunks";
    public static final String CLAIMS_BLOCK_EDIT_PREFIX = "serverutilities.claims.block.edit";
    public static final String CLAIMS_BLOCK_INTERACT_PREFIX = "serverutilities.claims.block.interact";
    public static final String CLAIMS_ITEM_PREFIX = "serverutilities.claims.item";
    public static final String CLAIMS_BYPASS_LIMITS = "serverutilities.claims.bypass_limits";
    public static final String CLAIMS_ATTACK_ANIMALS = "serverutilities.claims.attack_animals";
    public static final String CLAIM_DECAY_TIMER = "serverutilities.claims.decay";

    public static final HashSet<Block> CLAIMS_BLOCK_EDIT_WHITELIST = new HashSet<>();
    public static final HashSet<Block> CLAIMS_BLOCK_INTERACT_WHITELIST = new HashSet<>();
    public static final HashSet<Item> CLAIMS_ITEM_BLACKLIST = new HashSet<>();

    // Chunkloader //
    public static final String CHUNKLOADER_MAX_CHUNKS = "serverutilities.chunkloader.max_chunks";
    public static final String CHUNKLOADER_LOAD_OFFLINE = "serverutilities.chunkloader.load_offline";
    public static final String CHUNKLOAD_DECAY_TIMER = "serverutilities.chunkloader.decay";

    // Chat //
    public static final String CHAT_SPEAK = "serverutilities.chat.speak";
    public static final String CHAT_FORMATTING = "serverutilities.chat.formatting";
    public static final String CHAT_NICKNAME_SET = "serverutilities.chat.nickname.set";
    public static final String CHAT_NICKNAME_COLORS = "serverutilities.chat.nickname.colors";
    public static final String CHAT_NAME_FORMAT = "serverutilities.chat.name_format";
    public static final String CHAT_TEXT_COLOR = "serverutilities.chat.text.color";
    public static final String CHAT_TEXT_BOLD = "serverutilities.chat.text.bold";
    public static final String CHAT_TEXT_ITALIC = "serverutilities.chat.text.italic";
    public static final String CHAT_TEXT_UNDERLINED = "serverutilities.chat.text.underlined";
    public static final String CHAT_TEXT_STRIKETHROUGH = "serverutilities.chat.text.strikethrough";
    public static final String CHAT_TEXT_OBFUSCATED = "serverutilities.chat.text.obfuscated";

    // Other //
    public static final String INFINITE_BACK_USAGE = "serverutilities.back.infinite";
    public static final String CRASH_REPORTS_VIEW = "admin_panel.serverutilities.crash_reports.view";
    public static final String CRASH_REPORTS_DELETE = "admin_panel.serverutilities.crash_reports.delete";
    private static final String LEADERBOARD_PREFIX = "serverutilities.leaderboard.";
    public static final String EDIT_WORLD_GAMERULES = "admin_panel.serverutilities.edit_world.gamerules";
    public static final String RANK_EDIT = "serverutilities.admin_panel.ranks.view";

    public static final String TPA_COOLDOWN = "serverutilities.tpa.cooldown";
    public static final String SPAWN_COOLDOWN = "serverutilities.spawn.cooldown";
    public static final String BACK_COOLDOWN = "serverutilities.back.cooldown";
    public static final String RTP_COOLDOWN = "serverutilities.rtp.cooldown";

    public static final String TPA_WARMUP = "serverutilities.tpa.warmup";
    public static final String SPAWN_WARMUP = "serverutilities.spawn.warmup";
    public static final String BACK_WARMUP = "serverutilities.back.warmup";
    public static final String RTP_WARMUP = "serverutilities.rtp.warmup";

    public static final String TPA_CROSS_DIM = "serverutilities.tpa.cross_dim";
    public static final String AFK_TIMER = "serverutilities.afk.timer";
    public static final String HEAL_OTHER = "serverutilities.other_player.heal";

    public static final String CLAIMS_JOURNEYMAP = "serverutilities.journeymap.enable";
    public static final String CLAIMS_JOURNEYMAP_OTHER = "serverutilities.journeymap.other";

    @SubscribeEvent
    public void registerRankConfigHandler(RegisterRankConfigHandlerEvent event) {
        if (ServerUtilitiesConfig.ranks.enabled) {
            event.setHandler(ServerUtilitiesPermissionHandler.INSTANCE);
        }
    }

    public static void registerPermissions() {
        PermissionAPI.registerNode(CHAT_SPEAK, DefaultPermissionLevel.ALL, "Controls if player is muted or not");
        PermissionAPI.registerNode(
                CHAT_FORMATTING,
                DefaultPermissionLevel.ALL,
                "Allows to use **bold**, *italic* and ~~strikethrough~~ in chat");
        PermissionAPI.registerNode(CHAT_NICKNAME_SET, DefaultPermissionLevel.OP, "Allow to change nickname");
        PermissionAPI.registerNode(
                CHAT_NICKNAME_COLORS,
                DefaultPermissionLevel.OP,
                "Allow to use formatting codes in nickname, requires " + CHAT_NICKNAME_SET);
        PermissionAPI.registerNode(DISPLAY_ADMIN_INFO, DefaultPermissionLevel.OP, "Display 'Admin' in Server Info");
        PermissionAPI.registerNode(
                HOMES_CROSS_DIM,
                DefaultPermissionLevel.ALL,
                "Can use /home to teleport to/from another dimension");
        PermissionAPI.registerNode(HOMES_LIST_OTHER, DefaultPermissionLevel.OP, "Allow to list other people homes");
        PermissionAPI.registerNode(
                HOMES_TELEPORT_OTHER,
                DefaultPermissionLevel.OP,
                "Allow to teleport to other people homes");
        PermissionAPI.registerNode(
                CLAIMS_OTHER_SEE_INFO,
                DefaultPermissionLevel.OP,
                "Allow player to see info of other team chunks");
        PermissionAPI
                .registerNode(CLAIMS_OTHER_CLAIM, DefaultPermissionLevel.OP, "Allow player to claim other team chunks");
        PermissionAPI.registerNode(
                CLAIMS_OTHER_UNCLAIM,
                DefaultPermissionLevel.OP,
                "Allow player to unclaim other team chunks");
        PermissionAPI
                .registerNode(CLAIMS_OTHER_LOAD, DefaultPermissionLevel.OP, "Allow player to load other team chunks");
        PermissionAPI.registerNode(
                CLAIMS_OTHER_UNLOAD,
                DefaultPermissionLevel.OP,
                "Allow player to unload other team chunks");
        PermissionAPI.registerNode(
                CLAIMS_BYPASS_LIMITS,
                DefaultPermissionLevel.NONE,
                "Allow to bypass claiming and loading limits");
        PermissionAPI.registerNode(
                CLAIMS_ATTACK_ANIMALS,
                DefaultPermissionLevel.OP,
                "Allow to attack animals in claimed chunks");
        PermissionAPI.registerNode(
                CHUNKLOADER_LOAD_OFFLINE,
                DefaultPermissionLevel.ALL,
                "Keep loaded chunks working when player goes offline");
        PermissionAPI.registerNode(
                INFINITE_BACK_USAGE,
                DefaultPermissionLevel.NONE,
                "Allow to use 'back' command infinite times");
        PermissionAPI.registerNode(
                CRASH_REPORTS_VIEW,
                DefaultPermissionLevel.OP,
                "Allow to view crash reports via Admin Panel");
        PermissionAPI.registerNode(
                CRASH_REPORTS_DELETE,
                DefaultPermissionLevel.OP,
                "Allow to delete crash reports, requires " + CRASH_REPORTS_VIEW);
        PermissionAPI.registerNode(
                EDIT_WORLD_GAMERULES,
                DefaultPermissionLevel.OP,
                "Allow to edit gamerules via Admin Panel");
        PermissionAPI.registerNode(
                TPA_CROSS_DIM,
                DefaultPermissionLevel.ALL,
                "Can use /tpa to teleport to/from another dimension");
        PermissionAPI.registerNode(HEAL_OTHER, DefaultPermissionLevel.OP, "Allow to heal other players");
        PermissionAPI.registerNode(
                HOMES_BACK,
                DefaultPermissionLevel.OP,
                "Allow player back to last time where /home is used");
        PermissionAPI.registerNode(
                WARPS_BACK,
                DefaultPermissionLevel.OP,
                "Allow player back to last time where /warp is used");
        PermissionAPI.registerNode(
                BACK_BACK,
                DefaultPermissionLevel.OP,
                "Allow player back to last time where /back is used");
        PermissionAPI.registerNode(
                SPAWN_BACK,
                DefaultPermissionLevel.OP,
                "Allow player back to last time where /spawn is used");
        PermissionAPI
                .registerNode(TPA_BACK, DefaultPermissionLevel.OP, "Allow player back to last time where /tpa is used");
        PermissionAPI
                .registerNode(RTP_BACK, DefaultPermissionLevel.OP, "Allow player back to last time where /rtp is used");
        PermissionAPI.registerNode(RESPAWN_BACK, DefaultPermissionLevel.ALL, "Allow player back to last death point");
        PermissionAPI.registerNode(
                CLAIMS_JOURNEYMAP,
                DefaultPermissionLevel.ALL,
                "Allow player to see own teams claims on JourneyMap overlay");
        PermissionAPI.registerNode(
                CLAIMS_JOURNEYMAP_OTHER,
                DefaultPermissionLevel.ALL,
                "Allow player to see other teams claims on JourneyMap overlay");
        PermissionAPI.registerNode(RANK_EDIT, DefaultPermissionLevel.OP, "Allow player to edit ranks via Admin Panel");

        for (Block block : GameData.getBlockRegistry().typeSafeIterable()) {
            String name = formatId(block);

            if (name.endsWith(".grave") || name.endsWith(".gravestone")) {
                CLAIMS_BLOCK_EDIT_WHITELIST.add(block);
            }

            if (block instanceof BlockDoor || block instanceof BlockWorkbench || block instanceof BlockAnvil) {
                CLAIMS_BLOCK_INTERACT_WHITELIST.add(block);
            }
        }

        for (Item item : GameData.getItemRegistry().typeSafeIterable()) {
            if (item instanceof ItemBucket) {
                CLAIMS_ITEM_BLACKLIST.add(item);
            }
        }

        for (Block block : GameData.getBlockRegistry().typeSafeIterable()) {
            String name = formatId(block);
            PermissionAPI.registerNode(
                    CLAIMS_BLOCK_EDIT_PREFIX + '.' + name,
                    CLAIMS_BLOCK_EDIT_WHITELIST.contains(block) ? DefaultPermissionLevel.ALL
                            : DefaultPermissionLevel.OP,
                    "");
            PermissionAPI.registerNode(
                    CLAIMS_BLOCK_INTERACT_PREFIX + '.' + name,
                    CLAIMS_BLOCK_INTERACT_WHITELIST.contains(block) ? DefaultPermissionLevel.ALL
                            : DefaultPermissionLevel.OP,
                    "");
        }

        for (Item item : GameData.getItemRegistry().typeSafeIterable()) {
            PermissionAPI.registerNode(
                    CLAIMS_ITEM_PREFIX + '.' + formatId(item),
                    CLAIMS_ITEM_BLACKLIST.contains(item) ? DefaultPermissionLevel.OP : DefaultPermissionLevel.ALL,
                    "");
        }

        for (Leaderboard leaderboard : ServerUtilitiesCommon.LEADERBOARDS.values()) {
            PermissionAPI.registerNode(getLeaderboardNode(leaderboard), DefaultPermissionLevel.ALL, "");
        }
    }

    @SubscribeEvent
    public void registerConfigs(RegisterRankConfigEvent event) {
        event.register(
                Rank.NODE_PARENT,
                new ConfigString("", Pattern.compile("^[a-z0-9\\s,]*$")),
                new ConfigString(""));
        event.register(Rank.NODE_DEFAULT_PLAYER, new ConfigBoolean(false), new ConfigBoolean(false));
        event.register(Rank.NODE_DEFAULT_OP, new ConfigBoolean(false), new ConfigBoolean(false));
        event.register(Rank.NODE_POWER, new ConfigInt(0, 0, Integer.MAX_VALUE - 1), new ConfigInt(0));
        event.register(Rank.NODE_PRIORITY, new ConfigInt(0, 0, Integer.MAX_VALUE - 1), new ConfigInt(0));

        event.register(CHAT_NAME_FORMAT, new ConfigString("<{name}>"), new ConfigString("<&2{name}&r>"));
        event.register(CHAT_TEXT_COLOR, new ConfigEnum<>(TextComponentParser.TEXT_FORMATTING_COLORS_NAME_MAP));
        event.register(HOMES_MAX, new ConfigInt(1, 0, 30000), new ConfigInt(100));
        event.register(HOMES_COOLDOWN, new ConfigTimer(Ticks.MINUTE.x(5)), new ConfigTimer(Ticks.NO_TICKS));
        event.register(WARPS_COOLDOWN, new ConfigTimer(Ticks.MINUTE), new ConfigTimer(Ticks.NO_TICKS));
        event.register(TPA_COOLDOWN, new ConfigTimer(Ticks.MINUTE.x(3)), new ConfigTimer(Ticks.NO_TICKS));
        event.register(SPAWN_COOLDOWN, new ConfigTimer(Ticks.MINUTE), new ConfigTimer(Ticks.NO_TICKS));
        event.register(BACK_COOLDOWN, new ConfigTimer(Ticks.MINUTE.x(3)), new ConfigTimer(Ticks.NO_TICKS));
        event.register(RTP_COOLDOWN, new ConfigTimer(Ticks.MINUTE.x(10)), new ConfigTimer(Ticks.NO_TICKS));
        event.register(
                HOMES_WARMUP,
                new ConfigTimer(Ticks.SECOND.x(5), Ticks.MINUTE.x(5)),
                new ConfigTimer(Ticks.NO_TICKS));
        event.register(
                WARPS_WARMUP,
                new ConfigTimer(Ticks.SECOND.x(5), Ticks.MINUTE.x(5)),
                new ConfigTimer(Ticks.NO_TICKS));
        event.register(
                TPA_WARMUP,
                new ConfigTimer(Ticks.SECOND.x(5), Ticks.MINUTE.x(5)),
                new ConfigTimer(Ticks.NO_TICKS));
        event.register(
                SPAWN_WARMUP,
                new ConfigTimer(Ticks.SECOND.x(5), Ticks.MINUTE.x(5)),
                new ConfigTimer(Ticks.NO_TICKS));
        event.register(
                BACK_WARMUP,
                new ConfigTimer(Ticks.SECOND.x(5), Ticks.MINUTE.x(5)),
                new ConfigTimer(Ticks.NO_TICKS));
        event.register(
                RTP_WARMUP,
                new ConfigTimer(Ticks.SECOND.x(5), Ticks.MINUTE.x(5)),
                new ConfigTimer(Ticks.NO_TICKS));
        event.register(CLAIMS_MAX_CHUNKS, new ConfigInt(100, 0, 30000), new ConfigInt(1000));
        event.register(CHUNKLOADER_MAX_CHUNKS, new ConfigInt(50, 0, 30000), new ConfigInt(64));
        event.register(AFK_TIMER, new ConfigTimer(Ticks.NO_TICKS));
        event.register(CLAIM_DECAY_TIMER, new ConfigTimer(Ticks.NO_TICKS, Ticks.DAY.x(365)));
        event.register(
                CHUNKLOAD_DECAY_TIMER,
                new ConfigTimer(Ticks.WEEK.x(2), Ticks.DAY.x(365)),
                new ConfigTimer(Ticks.NO_TICKS));
    }

    @SubscribeEvent
    public void registerCustomPermissionPrefixes(CustomPermissionPrefixesRegistryEvent event) {
        event.register(
                Rank.NODE_COMMAND,
                DefaultPermissionLevel.OP,
                "Permission for commands, if ServerUtilities command overriding is enabled. If not, this String will be inactive");
        event.register(
                CLAIMS_BLOCK_EDIT_PREFIX,
                DefaultPermissionLevel.OP,
                "Permission for blocks that players can break and place within claimed chunks");
        event.register(
                CLAIMS_BLOCK_INTERACT_PREFIX,
                DefaultPermissionLevel.OP,
                "Permission for blocks that players can right-click within claimed chunks");
        event.register(
                CLAIMS_ITEM_PREFIX,
                DefaultPermissionLevel.ALL,
                "Permission for items that players can right-click in air within claimed chunks");
        event.register(
                LEADERBOARD_PREFIX,
                DefaultPermissionLevel.ALL,
                "Permission for leaderboards that players can view");
    }

    public static String formatId(@Nullable Block item) {
        return (item == null || GameData.getBlockRegistry().getNameForObject(item) == null) ? "minecraft.air"
                : GameData.getBlockRegistry().getNameForObject(item).toLowerCase().replace(':', '.');
    }

    public static String formatId(@Nullable Item item) {
        return (item == null || GameData.getItemRegistry().getNameForObject(item) == null) ? "minecraft.air"
                : GameData.getItemRegistry().getNameForObject(item).toLowerCase().replace(':', '.');
    }

    public static boolean hasBlockEditingPermission(EntityPlayer player, Block block) {
        return PermissionAPI.hasPermission(player, CLAIMS_BLOCK_EDIT_PREFIX + '.' + formatId(block));
    }

    public static boolean hasBlockInteractionPermission(EntityPlayer player, Block block) {
        return PermissionAPI.hasPermission(player, CLAIMS_BLOCK_INTERACT_PREFIX + '.' + formatId(block));
    }

    public static boolean hasItemUsePermission(EntityPlayer player, Item block) {
        return PermissionAPI.hasPermission(player, CLAIMS_ITEM_PREFIX + '.' + formatId(block));
    }

    public static String getLeaderboardNode(Leaderboard leaderboard) {
        return LEADERBOARD_PREFIX + leaderboard.id.getResourceDomain() + "." + leaderboard.id.getResourcePath();
    }
}
