package serverutils.net;

import static serverutils.ServerUtilitiesPermissions.LEADERBOARD_PREFIX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.command.ICommand;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.IntComparators;
import serverutils.ServerUtilitiesPermissions;
import serverutils.client.gui.ranks.GuiRanks;
import serverutils.client.gui.ranks.RankInst;
import serverutils.data.NodeEntry;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.config.ConfigBoolean;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.ConfigValue;
import serverutils.lib.config.RankConfigAPI;
import serverutils.lib.config.RankConfigValueInfo;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.permission.DefaultPermissionHandler;
import serverutils.lib.util.permission.DefaultPermissionLevel;
import serverutils.lib.util.permission.PermissionAPI;
import serverutils.ranks.ICommandWithPermission;
import serverutils.ranks.PlayerRank;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;

public class MessageRanks extends MessageToClient {

    private Collection<RankInst> ranks;
    private Map<String, RankInst> playerRanks;
    private static ConfigGroup allPermissions;
    private static ConfigGroup commandPermissions;

    public MessageRanks() {}

    public MessageRanks(Ranks r, ForgePlayer p) {
        ranks = new ArrayList<>();

        for (Rank rank : r.ranks.values()) {
            ConfigGroup group = ConfigGroup.newGroup("");
            IChatComponent name = new ChatComponentText(
                    EnumChatFormatting.BOLD + StringUtils.firstUppercase(rank.getId()));
            group.setDisplayName(name);
            RankInst inst = new RankInst(rank.getId());

            for (Rank.Entry entry : rank.permissions.values()) {
                String permissionNode = entry.node;
                ConfigValue val = rank.getPermissionValue(permissionNode);
                ConfigValue defaultValue = RankConfigAPI.getConfigValue(permissionNode, false);
                group.add(permissionNode, val, defaultValue, StringUtils.FLAG_ID_PERIOD_DEFAULTS)
                        .setDisplayName(new ChatComponentTranslation(permissionNode));
            }

            inst.group = group;

            List<String> parents = new ArrayList<>();
            for (Rank rs : rank.getParents()) {
                parents.add(rs.getId());
            }

            inst.parents = parents;
            ranks.add(inst);

        }

        playerRanks = new HashMap<>();

        for (ForgePlayer player : r.universe.getPlayers()) {
            RankInst inst = new RankInst(player.getProfile().getId().toString());
            boolean isOp = ServerUtils.isOP(null, player.getProfile());
            inst.group = ConfigGroup.newGroup(player.getName());
            inst.group.add("is_op", new ConfigBoolean(isOp), null).setExcluded(true);
            inst.player = player.getName();

            PlayerRank pRank = r.getPlayerRank(player.getProfile());
            List<Rank> sortedParents = new ArrayList<>(pRank.getActualParents());
            sortedParents
                    .sort((r1, r2) -> IntComparators.OPPOSITE_COMPARATOR.compare(r1.getPriority(), r2.getPriority()));
            for (Rank rank : sortedParents) {
                inst.parents.add(rank.getId());
            }

            playerRanks.put(player.getName(), inst);
        }

        if (allPermissions == null) {
            allPermissions = ConfigGroup.newGroup("");
            for (RankConfigValueInfo info : RankConfigAPI.getHandler().getRegisteredConfigs()) {
                allPermissions.add(info.node, info.defaultValue, info.defaultValue, StringUtils.FLAG_ID_PERIOD_DEFAULTS)
                        .setDisplayName(new ChatComponentTranslation(info.node));
            }

            Collection<NodeEntry> prefixesToSkip = ServerUtilitiesPermissions.getPrefixesExcluding(LEADERBOARD_PREFIX);

            for (String node : PermissionAPI.getPermissionHandler().getRegisteredNodes()) {
                DefaultPermissionLevel level = DefaultPermissionHandler.INSTANCE.getDefaultPermissionLevel(node);
                String desc = PermissionAPI.getPermissionHandler().getNodeDescription(node);
                boolean printNode = true;

                if (node.startsWith(Rank.NODE_COMMAND)) continue;
                for (NodeEntry cprefix : prefixesToSkip) {
                    if (node.startsWith(cprefix.getNode())) {
                        if (cprefix.level != null && level == cprefix.level && desc.isEmpty()) {
                            printNode = false;
                        }
                        break;
                    }
                }

                if (printNode) {
                    ConfigValue val = new ConfigBoolean(level == DefaultPermissionLevel.ALL);
                    allPermissions.add(node, val, val, StringUtils.FLAG_ID_PERIOD_DEFAULTS)
                            .setDisplayName(new ChatComponentTranslation(node));
                }
            }
        }

        if (commandPermissions == null) {
            commandPermissions = ConfigGroup.newGroup("");
            for (ICommandWithPermission command : CommandUtils.getPermissionCommands()) {
                String node = command.serverutilities$getPermissionNode();
                DefaultPermissionLevel level = DefaultPermissionHandler.INSTANCE.getDefaultPermissionLevel(node);
                IChatComponent name = new ChatComponentText(
                        EnumChatFormatting.BLUE + "[" + command.serverutilities$getModName() + "]\n");
                ConfigBoolean val = new ConfigBoolean(level == DefaultPermissionLevel.ALL);
                commandPermissions.add(node, val, val, StringUtils.FLAG_ID_PERIOD_DEFAULTS)
                        .setDisplayName(new ChatComponentTranslation(node)).setInfo(
                                name.appendSibling(CommandUtils.getTranslatedUsage((ICommand) command, p.getPlayer())));
            }
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.FILES;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeCollection(ranks, RankInst.SERIALIZER);
        data.writeMap(playerRanks, DataOut.STRING, RankInst.SERIALIZER);
        ConfigGroup.SERIALIZER.write(data, allPermissions);
        ConfigGroup.SERIALIZER.write(data, commandPermissions);
    }

    @Override
    public void readData(DataIn data) {
        ranks = data.readCollection(RankInst.DESERIALIZER);
        playerRanks = data.readMap(DataIn.STRING, RankInst.DESERIALIZER);
        allPermissions = ConfigGroup.DESERIALIZER.read(data);
        commandPermissions = ConfigGroup.DESERIALIZER.read(data);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new GuiRanks(ranks, playerRanks, allPermissions, commandPermissions).openGui();
    }
}
