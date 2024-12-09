package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import serverutils.ServerUtilities;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.util.StringUtils;

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
    }
}
