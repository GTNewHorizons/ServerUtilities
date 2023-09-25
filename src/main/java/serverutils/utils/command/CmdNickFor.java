package serverutils.utils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;

import com.feed_the_beast.ftblib.lib.command.CmdBase;
import com.feed_the_beast.ftblib.lib.command.CommandUtils;
import com.feed_the_beast.ftblib.lib.data.ForgePlayer;
import com.feed_the_beast.ftblib.lib.util.StringUtils;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesConfig;
import serverutils.utils.data.FTBUtilitiesPlayerData;
import serverutils.utils.net.MessageUpdateTabName;

public class CmdNickFor extends CmdBase {

    public CmdNickFor() {
        super("nickfor", Level.OP);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        checkArgs(sender, args, 2);
        ForgePlayer player = CommandUtils.getForgePlayer(sender, args[0]);

        FTBUtilitiesPlayerData data = FTBUtilitiesPlayerData.get(player);
        data.setNickname(StringUtils.joinSpaceUntilEnd(1, args).trim());

        if (data.getNickname().isEmpty()) {
            sender.addChatMessage(ServerUtilities.lang(sender, "ftbutilities.lang.nickname_reset"));
        } else {
            String name = StringUtils.addFormatting(data.getNickname());

            if (name.indexOf(StringUtils.FORMATTING_CHAR) != -1) {
                name += EnumChatFormatting.RESET;
            }

            sender.addChatMessage(ServerUtilities.lang(sender, "ftbutilities.lang.nickname_changed", name));
        }

        if (ServerUtilitiesConfig.chat.replace_tab_names) {
            new MessageUpdateTabName(player.getPlayer()).sendToAll();
        }
    }
}
