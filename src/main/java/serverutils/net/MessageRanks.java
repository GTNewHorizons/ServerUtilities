package serverutils.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.ServerUtilitiesCommon;
import serverutils.ServerUtilitiesConfig;
import serverutils.client.gui.ranks.GuiRanks;
import serverutils.client.gui.ranks.RankInst;
import serverutils.data.NodeEntry;
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
import serverutils.ranks.CommandOverride;
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

                if (ServerUtilitiesConfig.ranks.override_commands && permissionNode.startsWith("command.")) {
                    CommandOverride cmd = Ranks.INSTANCE.commands.get(permissionNode);
                    if (cmd == null) continue;
                    defaultValue = new ConfigBoolean(cmd.getRequiredPermissionLevel() == 0);
                    group.add(permissionNode, val, defaultValue, StringUtils.FLAG_ID_PERIOD_DEFAULTS)
                            .setDisplayName(new ChatComponentTranslation(permissionNode))
                            .setInfo(cmd.getTranslatedUsage(p.getPlayer()));
                } else {
                    group.add(permissionNode, val, defaultValue, StringUtils.FLAG_ID_PERIOD_DEFAULTS)
                            .setDisplayName(new ChatComponentTranslation(permissionNode));
                }
            }

            inst.group = group;

            Collection<String> parents = new HashSet<>();
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
            for (Rank rank : pRank.getActualParents()) {
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

            for (String s : PermissionAPI.getPermissionHandler().getRegisteredNodes()) {
                DefaultPermissionLevel level = DefaultPermissionHandler.INSTANCE.getDefaultPermissionLevel(s);
                String desc = PermissionAPI.getPermissionHandler().getNodeDescription(s);
                boolean printNode = true;

                for (NodeEntry cprefix : ServerUtilitiesCommon.CUSTOM_PERM_PREFIX_REGISTRY) {
                    if (s.startsWith(cprefix.getNode())) {
                        if (cprefix.level != null && level == cprefix.level && desc.isEmpty()) {
                            printNode = false;
                        }
                        break;
                    }
                }

                if (printNode) {
                    ConfigValue val = new ConfigBoolean(level == DefaultPermissionLevel.ALL);
                    allPermissions.add(s, val, val, StringUtils.FLAG_ID_PERIOD_DEFAULTS)
                            .setDisplayName(new ChatComponentTranslation(s));
                }
            }
        }

        if (commandPermissions == null) {
            commandPermissions = ConfigGroup.newGroup("");
            if (ServerUtilitiesConfig.ranks.override_commands) {
                for (CommandOverride command : Ranks.INSTANCE.commands.values()) {
                    String commandNode = command.node;
                    ConfigBoolean val = new ConfigBoolean(command.getRequiredPermissionLevel() == 0);

                    commandPermissions.add(commandNode, val, val, StringUtils.FLAG_ID_PERIOD_DEFAULTS)
                            .setDisplayName(new ChatComponentTranslation(commandNode))
                            .setInfo(command.getTranslatedUsage(p.getPlayer()));
                }
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
