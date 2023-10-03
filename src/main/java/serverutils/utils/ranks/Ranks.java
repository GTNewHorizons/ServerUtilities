package serverutils.utils.ranks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.Event;
import serverutils.lib.lib.config.ConfigNull;
import serverutils.lib.lib.config.ConfigValue;
import serverutils.lib.lib.config.RankConfigAPI;
import serverutils.lib.lib.config.RankConfigValueInfo;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.Universe;
import serverutils.lib.lib.io.DataReader;
import serverutils.lib.lib.util.FileUtils;
import serverutils.lib.lib.util.StringUtils;
import serverutils.lib.lib.util.permission.DefaultPermissionHandler;
import serverutils.lib.lib.util.permission.DefaultPermissionLevel;
import serverutils.lib.lib.util.permission.PermissionAPI;
import serverutils.mod.ServerUtilities;
import serverutils.mod.ServerUtilitiesCommon;
import serverutils.mod.ServerUtilitiesConfig;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.NodeEntry;

public class Ranks {

    public static Ranks INSTANCE;
    public static Pattern RANK_NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");

    public static boolean isActive() {
        return ServerUtilitiesConfig.ranks.enabled && INSTANCE != null
                && PermissionAPI.getPermissionHandler() == ServerUtilitiesPermissionHandler.INSTANCE;
    }

    public static boolean isValidName(@Nullable String id) {
        return id != null && !id.isEmpty() && !id.equals("none") && RANK_NAME_PATTERN.matcher(id).matches();
    }

    public final Universe universe;
    public final Map<String, Rank> ranks;
    private Collection<String> rankNames;
    private Collection<String> permissionNodes;
    public final Map<UUID, PlayerRank> playerRanks;
    private Optional<Rank> defaultPlayerRank, defaultOPRank;
    public final Map<String, CommandOverride> commands;
    private File ranksFile, playersFile;

    public Ranks(Universe u) {
        universe = u;
        ranks = new LinkedHashMap<>();
        rankNames = null;
        permissionNodes = null;
        playerRanks = new LinkedHashMap<>();
        defaultPlayerRank = null;
        defaultOPRank = null;
        commands = new LinkedHashMap<>();
        ranksFile = null;
        playersFile = null;
    }

    public boolean reload() {
        ranks.clear();
        playerRanks.clear();
        clearCache();

        if (!isActive()) {
            return true;
        }

        boolean save = false;

        ranksFile = universe.server.getFile("server utilities/serverutilities/ranks.txt");

        if (!ranksFile.exists()) {
            Rank pRank = new Rank(this, "player");
            pRank.add();
            pRank.setPermission(Rank.NODE_DEFAULT_PLAYER, true);
            pRank.setPermission(Rank.NODE_POWER, 1);
            pRank.setPermission("example.permission", true);
            pRank.setPermission("example.other_permission", false);
            pRank.setPermission("example.permission_with_value", 0);

            Rank vRank = new Rank(this, "vip");
            vRank.add();
            vRank.setPermission(Rank.NODE_POWER, 20);
            vRank.setPermission("serverutilities.chat.name_format", "<&bVIP {name}&r>");
            vRank.setPermission("example.other_permission", true);
            vRank.setPermission("example.permission_with_value", 15);

            Rank aRank = new Rank(this, "admin");
            aRank.add();
            aRank.setPermission(Rank.NODE_DEFAULT_OP, true);
            aRank.setPermission(Rank.NODE_POWER, 100);
            aRank.setPermission("serverutilities.chat.name_format", "<&2{name}&r>");
            aRank.setPermission("example.permission_with_value", 100);

            PlayerRank fpRank = new PlayerRank(
                    this,
                    UUID.fromString("069be141-3c1b-45c3-b3b1-60d3f9fcd236"),
                    "FakeForgePlayer");
            fpRank.add();
            fpRank.addParent(vRank);
            fpRank.setPermission("example.permission_with_value", 150);
            save = true;
        }

        Rank currentRank = null;
        String lastComment = "";

        for (String line : DataReader.get(ranksFile).safeStringList()) {
            if (line.isEmpty()) {
                lastComment = "";
            } else if (line.startsWith("//")) {
                lastComment = line.substring(2).trim();
            } else if (line.startsWith("[") && line.endsWith("]")) {
                String linein = line.substring(1, line.length() - 1);

                if (linein.isEmpty()) {
                    currentRank = null;
                    continue;
                }

                String[] iss = linein.split(" is ", 2);
                String[] extendss = iss[0].split(" extends ", 2);

                String rankID = StringUtils.removeAllWhitespace(extendss[0]);

                if (rankID.isEmpty()) {
                    currentRank = null;
                    continue;
                }

                UUID rankUUID = StringUtils.fromString(rankID);

                if (rankUUID != null && universe.getPlayer(rankUUID) != null) {
                    currentRank = getPlayerRank(universe.getPlayer(rankUUID).getProfile());
                } else {
                    currentRank = new Rank(this, rankID);
                }

                currentRank.comment = lastComment;
                lastComment = "";

                if (!currentRank.isPlayer()) {
                    if (isValidName(currentRank.getId())) {
                        currentRank.add();
                        currentRank.setPermission(Rank.NODE_POWER, String.valueOf(ranks.size()));
                    } else {
                        currentRank = null;
                        continue;
                    }
                } else {
                    save = true;
                }

                if (extendss.length == 2) {
                    currentRank.setPermission(Rank.NODE_PARENT, StringUtils.removeAllWhitespace(extendss[1]));
                    save = true;
                }

                if (iss.length == 2) {
                    for (String tag : iss[1].split(",")) {
                        String s = StringUtils.removeAllWhitespace(tag);

                        if (!s.isEmpty()) {
                            currentRank.setPermission(s, true);
                            save = true;
                        }
                    }
                }
            } else if (currentRank != null) {
                String[] s1 = line.split(":", 2);

                if (s1.length == 2) {
                    String value = s1[1].trim();

                    if (value.length() > 2 && value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                        save = true;
                    }

                    if (!value.isEmpty()) {
                        Rank.Entry entry = currentRank.setPermission(s1[0].trim(), value);

                        if (entry != null) {
                            entry.comment = lastComment;
                        }
                    }
                }

                lastComment = "";
            }
        }

        File oldPlayerRanksFile = universe.server.getFile("server utilities/serverutilities/player_ranks.txt");

        if (oldPlayerRanksFile.exists()) {
            for (String s : DataReader.get(oldPlayerRanksFile).safeStringList()) {
                if (s.isEmpty() || s.startsWith("//")) {
                    continue;
                }

                String[] s1 = s.split(":", 2);

                if (s1.length == 2) {
                    ForgePlayer player = universe.getPlayer(s1[0].trim());

                    if (player != null) {
                        Rank rank = getRank(s1[1].trim());

                        if (rank != null && !rank.isPlayer()) {
                            getPlayerRank(player.getProfile()).addParent(rank);
                        }
                    }
                }
            }

            oldPlayerRanksFile.delete();
            save = true;
        }

        File badgeFile = universe.server.getFile("server utilities/serverutilities/server_badges.txt");

        if (badgeFile.exists()) {
            for (String s : DataReader.get(badgeFile).safeStringList()) {
                if (s.isEmpty() || s.startsWith("//")) {
                    continue;
                }

                String[] s1 = s.trim().split(":", 2);

                if (s1.length == 2) {
                    ForgePlayer player = universe.getPlayer(s1[0].trim());

                    if (player != null) {
                        getPlayerRank(player.getProfile())
                                .setPermission(ServerUtilitiesPermissions.BADGE, s1[1].trim());
                    }
                }
            }

            badgeFile.delete();
            save = true;
        }

        playersFile = universe.server.getFile("server utilities/serverutilities/players.txt");

        currentRank = null;
        lastComment = "";

        for (String line : DataReader.get(playersFile).safeStringList()) {
            if (line.isEmpty()) {
                lastComment = "";
            } else if (line.startsWith("//")) {
                lastComment = line.substring(2).trim();
            } else if (line.startsWith("[") && line.endsWith("]")) {
                String linein = line.substring(1, line.length() - 1);

                if (linein.isEmpty()) {
                    currentRank = null;
                    continue;
                }

                String[] iss = linein.split(" is ", 2);
                String[] extendss = iss[0].split(" extends ", 2);

                String rankID = StringUtils.removeAllWhitespace(extendss[0]);

                if (rankID.isEmpty()) {
                    currentRank = null;
                    continue;
                }

                UUID rankUUID = StringUtils.fromString(rankID);

                if (rankUUID != null && universe.getPlayer(rankUUID) != null) {
                    currentRank = getPlayerRank(universe.getPlayer(rankUUID).getProfile());
                } else {
                    currentRank = new Rank(this, rankID);
                }

                currentRank.comment = lastComment;
                lastComment = "";

                if (!currentRank.isPlayer()) {
                    if (isValidName(currentRank.getId())) {
                        currentRank.add();
                        currentRank.setPermission(Rank.NODE_POWER, String.valueOf(ranks.size()));
                    } else {
                        currentRank = null;
                        continue;
                    }
                }

                if (extendss.length == 2) {
                    currentRank.setPermission(Rank.NODE_PARENT, StringUtils.removeAllWhitespace(extendss[1]));
                    save = true;
                }

                if (iss.length == 2) {
                    for (String tag : iss[1].split(",")) {
                        String s = StringUtils.removeAllWhitespace(tag);

                        if (!s.isEmpty()) {
                            currentRank.setPermission(s, true);
                            save = true;
                        }
                    }
                }
            } else if (currentRank != null) {
                String[] s1 = line.split(":", 2);

                if (s1.length == 2) {
                    String value = s1[1].trim();

                    if (!value.isEmpty()) {
                        Rank.Entry entry = currentRank.setPermission(s1[0].trim(), value);

                        if (entry != null) {
                            entry.comment = lastComment;
                        }
                    }
                }

                lastComment = "";
            }
        }

        for (Rank rank : playerRanks.values()) {
            if (rank.setPermission(Rank.NODE_POWER, "") != null) {
                save = true;
            }
        }

        if (save) {
            save();
        }

        return true;
    }

    public void save() {
        universe.clearCache();

        List<String> list = new ArrayList<>();
        list.add("// For more info visit https://github.com/GTNewHorizons/ServerUtilities");

        for (Rank rank : ranks.values()) {
            if (rank.permissions.isEmpty()) {
                continue;
            }

            list.add("");

            if (!rank.comment.isEmpty()) {
                list.add("// " + rank.comment);
            }

            list.add("[" + rank.getId() + "]");

            for (Rank.Entry entry : rank.permissions.values()) {
                if (!entry.comment.isEmpty()) {
                    list.add("// " + entry.comment);
                }

                list.add(entry.node + ": " + entry.value);
            }
        }

        FileUtils.saveSafe(ranksFile, list);

        list = new ArrayList<>();
        list.add("// For more info visit https://github.com/GTNewHorizons/ServerUtilities");

        for (Rank rank : playerRanks.values()) {
            if (rank.permissions.isEmpty()) {
                continue;
            }

            list.add("");

            if (!rank.comment.isEmpty()) {
                list.add("// " + rank.comment);
            }

            list.add("[" + rank.getId() + "]");

            for (Rank.Entry entry : rank.permissions.values()) {
                if (!entry.comment.isEmpty()) {
                    list.add("// " + entry.comment);
                }

                list.add(entry.node + ": " + entry.value);
            }
        }

        FileUtils.saveSafe(playersFile, list);
    }

    public Rank getRank(ICommandSender sender, String id) throws CommandException {
        if (id.startsWith("@")) {
            return getPlayerRank(CommandBase.getPlayer(sender, id));
        }

        Rank r = getRank(id);

        if (r == null) {
            throw ServerUtilities.error(sender, "commands.ranks.not_found", id);
        }

        return r;
    }

    @Nullable
    public Rank getRank(String id) {
        if (id.isEmpty() || id.equals("none")) {
            return null;
        }

        Rank rank = ranks.get(id);

        if (rank == null) {
            ForgePlayer player = universe.getPlayer(id);

            if (player != null) {
                return getPlayerRank(player.getProfile());
            }
        }

        return rank;
    }

    @Nullable
    public Rank getDefaultPlayerRank() {
        if (defaultPlayerRank == null) {
            for (Rank rank : ranks.values()) {
                if (rank.isDefaultPlayerRank()) {
                    defaultPlayerRank = Optional.of(rank);
                    return rank;
                }
            }

            int power = Integer.MAX_VALUE;

            for (Rank rank : ranks.values()) {
                if (rank.getPower() <= power) {
                    power = rank.getPower();
                    defaultPlayerRank = Optional.of(rank);
                }
            }

            if (defaultPlayerRank == null) {
                defaultPlayerRank = Optional.empty();
            }
        }

        return defaultPlayerRank.orElse(null);
    }

    @Nullable
    public Rank getDefaultOPRank() {
        if (defaultOPRank == null) {
            for (Rank rank : ranks.values()) {
                if (rank.isDefaultOPRank()) {
                    defaultOPRank = Optional.of(rank);
                    return rank;
                }
            }

            int power = 0;

            for (Rank rank : ranks.values()) {
                if (rank.getPower() >= power) {
                    power = rank.getPower();
                    defaultOPRank = Optional.of(rank);
                }
            }

            if (defaultOPRank == null) {
                defaultOPRank = Optional.empty();
            }
        }

        return defaultOPRank.orElse(null);
    }

    public PlayerRank getPlayerRank(GameProfile profile) {
        UUID id = profile.getId();

        if (id == null) {
            throw new NullPointerException("Null UUID in profile " + profile.getName() + "!");
        }

        PlayerRank rank = playerRanks.get(id);

        if (rank == null) {
            rank = new PlayerRank(this, id, profile.getName() == null ? "" : profile.getName());
            rank.add();
        }

        return rank;
    }

    public PlayerRank getPlayerRank(EntityPlayer player) {
        return getPlayerRank(player.getGameProfile());
    }

    public ConfigValue getPermission(GameProfile profile, String node, boolean recursive) {
        if (!isActive() || profile.getId() == null) {
            return ConfigNull.INSTANCE;
        }

        return getPlayerRank(profile).getPermissionValue(node, node, recursive);
    }

    public ConfigValue getPermission(EntityPlayerMP player, String node, boolean recursive) {
        return getPermission(player.getGameProfile(), node, recursive);
    }

    public Event.Result getPermissionResult(GameProfile profile, String node, boolean recursive) {
        ConfigValue value = getPermission(profile, node, recursive);
        return value.isNull() ? Event.Result.DEFAULT : value.getBoolean() ? Event.Result.ALLOW : Event.Result.DENY;
    }

    public Event.Result getPermissionResult(EntityPlayerMP player, String node, boolean recursive) {
        return getPermissionResult(player.getGameProfile(), node, recursive);
    }

    public Collection<String> getPermissionNodes() {
        if (permissionNodes == null) {
            permissionNodes = new LinkedHashSet<>();

            for (String s : ServerUtilitiesPermissionHandler.INSTANCE.getRegisteredNodes()) {
                DefaultPermissionLevel level = DefaultPermissionHandler.INSTANCE.getDefaultPermissionLevel(s);
                String desc = DefaultPermissionHandler.INSTANCE.getNodeDescription(s);
                boolean printNode = true;

                for (NodeEntry entry : ServerUtilitiesCommon.CUSTOM_PERM_PREFIX_REGISTRY) {
                    if (s.startsWith(entry.getNode())) {
                        if (entry.level != null && level == entry.level && desc.isEmpty()) {
                            printNode = false;
                        }

                        break;
                    }
                }

                if (printNode) {
                    permissionNodes.add(s);
                }
            }

            for (NodeEntry entry : ServerUtilitiesCommon.CUSTOM_PERM_PREFIX_REGISTRY) {
                permissionNodes.add(entry.node);
            }

            for (String node : commands.keySet()) {
                permissionNodes.add(node);
            }

            for (RankConfigValueInfo info : RankConfigAPI.getHandler().getRegisteredConfigs()) {
                permissionNodes.add(info.node);
            }

            permissionNodes = Arrays.asList(permissionNodes.toArray(StringUtils.EMPTY_ARRAY));
        }

        return permissionNodes;
    }

    public Collection<String> getRankNames(boolean includeNone) {
        if (!includeNone) {
            return ranks.keySet();
        }

        if (rankNames == null) {
            rankNames = new ArrayList<>(ranks.keySet());
            rankNames.add("none");
            rankNames = Arrays.asList(rankNames.toArray(StringUtils.EMPTY_ARRAY));
        }

        return rankNames;
    }

    public void clearCache() {
        rankNames = null;
        permissionNodes = null;
        defaultPlayerRank = null;
        defaultOPRank = null;

        for (Rank rank : ranks.values()) {
            rank.clearCache();
        }

        for (PlayerRank rank : playerRanks.values()) {
            rank.clearCache();
        }
    }

    public static List<String> matchPossibleNodes(String last, Collection<String> nodes) {
        Set<String> s = new HashSet<>();
        for (String node : nodes) {
            if (node.startsWith(last)) {
                int i;
                for (i = last.length(); i < node.length() && node.charAt(i) != '.'; i++) {}
                s.add(node.substring(0, i));
            }
        }
        return new ArrayList<>(s);
    }
}
