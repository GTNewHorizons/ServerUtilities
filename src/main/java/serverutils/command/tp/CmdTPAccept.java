package serverutils.command.tp;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.util.StringUtils;

public class CmdTPAccept extends CmdBase {

    public CmdTPAccept() {
        super("tpaccept", Level.ALL);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 1);
        EntityPlayerMP selfPlayer = getCommandSenderAsPlayer(sender);
        ServerUtilitiesPlayerData self = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(selfPlayer));
        ServerUtilitiesPlayerData other = ServerUtilitiesPlayerData.get(CommandUtils.getForgePlayer(sender, args[0]));

        IChatComponent selfName = StringUtils
                .color(new ChatComponentText(self.player.getPlayer().getDisplayName()), EnumChatFormatting.BLUE);
        IChatComponent otherName = StringUtils
                .color(new ChatComponentText(other.player.getPlayer().getDisplayName()), EnumChatFormatting.BLUE);

        if (self.player.equalsPlayer(other.player) || !other.player.isOnline()
                || !self.tpaRequestsFrom.contains(other.player)) {
            throw ServerUtilities.error(sender, "serverutilities.lang.tpa.no_request", otherName);
        }

        if (selfPlayer.dimension != other.player.getPlayer().dimension
                && !other.player.hasPermission(ServerUtilitiesPermissions.TPA_CROSS_DIM)) {
            other.player.getPlayer().addChatMessage(
                    StringUtils.color(
                            ServerUtilities.lang(other.player.getPlayer(), "serverutilities.lang.warps.cross_dim"),
                            EnumChatFormatting.RED));
            throw ServerUtilities.error(sender, "serverutilities.lang.warps.cross_dim", otherName);
        }

        self.tpaRequestsFrom.remove(other.player);

        IChatComponent component = ServerUtilities.lang(sender, "serverutilities.lang.tpa.request_accepted");
        component.getChatStyle().setChatHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ServerUtilities.lang(sender, "serverutilities.lang.tpa.from_to", otherName, selfName)));
        sender.addChatMessage(component);

        component = ServerUtilities.lang(other.player.getPlayer(), "serverutilities.lang.tpa.request_accepted");
        component.getChatStyle().setChatHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        ServerUtilities.lang(
                                other.player.getPlayer(),
                                "serverutilities.lang.tpa.from_to",
                                otherName,
                                selfName)));
        other.player.getPlayer().addChatMessage(component);
        other.teleport(TeleporterDimPos.of(selfPlayer), TeleportType.TPA, null);
    }
}
