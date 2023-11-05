package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.NBTUtils;

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
        String name = player.getDisplayName();

        if (nbt.getBoolean("god")) {
            nbt.removeTag("god");
            player.capabilities.disableDamage = false;
            sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.god_off", name));
        } else {
            nbt.setBoolean("god", true);
            player.capabilities.disableDamage = true;
            sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.god_on", name));
        }

        player.sendPlayerAbilities();
    }
}
