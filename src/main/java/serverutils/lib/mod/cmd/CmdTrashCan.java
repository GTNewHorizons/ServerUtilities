package serverutils.lib.mod.cmd;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.item.BasicInventory;
import serverutils.lib.mod.config.ServerUtilitiesLibConfigCmd;
import serverutils.lib.mod.config.ServerUtilitiesLibConfigCmdNames;

public class CmdTrashCan extends CommandLM {

    public CmdTrashCan() {
        super(
                ServerUtilitiesLibConfigCmdNames.trash_can.getAsString(),
                ServerUtilitiesLibConfigCmd.level_trash_can.get());
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
        ep.displayGUIChest(new BasicInventory(18) {

            public String getInventoryName() {
                return "Trash Can";
            }

            public boolean hasCustomInventoryName() {
                return true;
            }
        });
        return null;
    }
}
