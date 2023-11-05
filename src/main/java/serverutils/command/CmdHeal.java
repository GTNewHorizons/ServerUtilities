package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CommandUtils;

public class CmdHeal extends CmdBase {

    public CmdHeal() {
        super("heal", Level.OP);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = CommandUtils.getSelfOrOther(sender, args, 0, ServerUtilitiesPermissions.HEAL_OTHER)
                .getCommandPlayer(sender);
        String name = player.getDisplayName();
        player.setHealth(player.getMaxHealth());
        player.getFoodStats().addStats(40, 40F);
        player.extinguish();
        sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.heal", name));
    }
}
