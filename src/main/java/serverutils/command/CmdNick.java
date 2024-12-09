package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.util.StringUtils;

public class CmdNick extends CmdBase {

    public CmdNick() {
        super("nick", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        ForgePlayer player = CommandUtils.getForgePlayer(sender);

        if (!player.hasPermission(ServerUtilitiesPermissions.CHAT_NICKNAME_SET)) {
            throw new CommandException("commands.generic.permission");
        }

        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(player);
        data.setNickname(StringUtils.joinSpaceUntilEnd(0, args));

        if (data.getNickname().isEmpty()) {
            player.getPlayer()
                    .addChatMessage(ServerUtilities.lang(player.getPlayer(), "serverutilities.lang.nickname_reset"));
        } else {
            String name = StringUtils.addFormatting(data.getNickname());

            if (!player.hasPermission(ServerUtilitiesPermissions.CHAT_NICKNAME_COLORS)) {
                name = StringUtils.unformatted(name);
            } else if (name.indexOf(StringUtils.FORMATTING_CHAR) != -1) {
                name += EnumChatFormatting.RESET;
            }

            player.getPlayer().addChatMessage(
                    ServerUtilities.lang(player.getPlayer(), "serverutilities.lang.nickname_changed", name));
        }
    }
}
