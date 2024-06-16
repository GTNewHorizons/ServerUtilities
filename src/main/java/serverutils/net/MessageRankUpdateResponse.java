package serverutils.net;

import java.util.Collection;
import java.util.HashSet;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.ServerUtilitiesConfig;
import serverutils.client.gui.ranks.GuiRanks;
import serverutils.client.gui.ranks.RankInst;
import serverutils.lib.config.ConfigBoolean;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.ConfigValue;
import serverutils.lib.config.RankConfigAPI;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.StringUtils;
import serverutils.ranks.CommandOverride;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;

public class MessageRankUpdateResponse extends MessageToClient {

    private Collection<RankInst> ranks;

    public MessageRankUpdateResponse() {}

    public MessageRankUpdateResponse(Collection<Rank> requestedRanks, ICommandSender sender) {
        ranks = new HashSet<>();
        for (Rank rank : requestedRanks) {
            ConfigGroup group = ConfigGroup.newGroup("");
            IChatComponent name = new ChatComponentText(
                    EnumChatFormatting.BOLD + StringUtils.firstUppercase(rank.getId()));
            group.setDisplayName(name);
            RankInst inst = new RankInst(rank.getId());

            for (Rank.Entry entry : rank.permissions.values()) {
                String permissionNode = entry.node;
                ConfigValue val = rank.getPermissionValue(entry.node);
                ConfigValue defaultValue = RankConfigAPI.getConfigValue(entry.node, false);

                if (ServerUtilitiesConfig.ranks.override_commands && permissionNode.startsWith("command.")) {
                    CommandOverride cmd = Ranks.INSTANCE.commands.get(permissionNode);
                    if (cmd == null) continue;
                    defaultValue = new ConfigBoolean(cmd.getRequiredPermissionLevel() == 0);
                    group.add(permissionNode, val, defaultValue, StringUtils.FLAG_ID_PERIOD_DEFAULTS)
                            .setDisplayName(new ChatComponentTranslation(permissionNode))
                            .setInfo(cmd.getTranslatedUsage(sender));
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
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.FILES;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeCollection(ranks, RankInst.SERIALIZER);
    }

    @Override
    public void readData(DataIn data) {
        ranks = data.readCollection(RankInst.DESERIALIZER);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        for (RankInst inst : ranks) {
            if (inst.player.isEmpty()) {
                GuiRanks.ranks.put(inst.getId(), inst);
            } else {
                GuiRanks.playerRanks.put(inst.player, inst);
            }
        }
    }
}
