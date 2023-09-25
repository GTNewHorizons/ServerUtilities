package serverutils.utils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.util.StringUtils;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesConfig;
import serverutils.utils.ServerUtilitiesPermissions;
import serverutils.utils.data.ServerUtilitiesPlayerData;
import serverutils.utils.net.MessageUpdateTabName;

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

        if (ServerUtilitiesConfig.chat.replace_tab_names) {
            new MessageUpdateTabName(player.getPlayer()).sendToAll();
        }
    }
}
