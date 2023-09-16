package serverutils.lib.mod.cmd;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.*;

import latmod.lib.LMStringUtils;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.mod.config.ServerUtilitiesLibConfigCmd;
import serverutils.lib.mod.config.ServerUtilitiesLibConfigCmdNames;

public class CmdSetItemName extends CommandLM {

    public CmdSetItemName() {
        super(
                ServerUtilitiesLibConfigCmdNames.set_item_name.getAsString(),
                ServerUtilitiesLibConfigCmd.level_set_item_name.get());
    }

    public String getCommandUsage(ICommandSender ics) {
        return '/' + commandName + " <name...>";
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        checkArgs(args, 1);
        EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
        if (ep.inventory.getCurrentItem() != null) {
            ep.inventory.getCurrentItem().setStackDisplayName(LMStringUtils.unsplit(args, " "));
            ep.openContainer.detectAndSendChanges();
            return new ChatComponentText("Item name set to '" + ep.inventory.getCurrentItem().getDisplayName() + "'!");
        }

        return null;
    }
}
