package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.util.StatCollector;

import serverutils.lib.command.CmdBase;

public class CmdTrashCan extends CmdBase {

    public CmdTrashCan() {
        super("trash_can", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        getCommandSenderAsPlayer(sender).displayGUIChest(
                new InventoryBasic(StatCollector.translateToLocal("key.serverutilities.trash"), true, 36));
    }
}
