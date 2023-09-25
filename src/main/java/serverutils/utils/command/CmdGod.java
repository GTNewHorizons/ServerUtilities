package serverutils.utils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.util.NBTUtils;

public class CmdGod extends CmdBase {

    public CmdGod() {
        super("god", Level.OP);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = args.length == 0 ? getCommandSenderAsPlayer(sender) : getPlayer(sender, args[0]);
        NBTTagCompound nbt = NBTUtils.getPersistedData(player, true);

        if (nbt.getBoolean("god")) {
            nbt.removeTag("god");
            player.capabilities.disableDamage = false;
        } else {
            nbt.setBoolean("god", true);
            player.capabilities.disableDamage = true;
        }

        player.sendPlayerAbilities();
    }
}
