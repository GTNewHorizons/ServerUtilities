package serverutils.utils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CommandUtils;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.util.StringUtils;
import serverutils.mod.ServerUtilities;
import serverutils.mod.ServerUtilitiesConfig;
import serverutils.utils.data.ServerUtilitiesPlayerData;
import serverutils.utils.net.MessageUpdateTabName;

public class CmdNickFor extends CmdBase {

    public CmdNickFor() {
        super("nickfor", Level.OP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 2);
        ForgePlayer player = CommandUtils.getForgePlayer(sender, args[0]);

        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(player);
        data.setNickname(StringUtils.joinSpaceUntilEnd(1, args).trim());

        if (data.getNickname().isEmpty()) {
            sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.nickname_reset"));
        } else {
            String name = StringUtils.addFormatting(data.getNickname());

            if (name.indexOf(StringUtils.FORMATTING_CHAR) != -1) {
                name += EnumChatFormatting.RESET;
            }

            sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.nickname_changed", name));
        }

        if (ServerUtilitiesConfig.chat.replace_tab_names) {
            new MessageUpdateTabName(player.getPlayer()).sendToAll();
        }
    }
}
