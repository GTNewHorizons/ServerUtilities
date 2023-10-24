package serverutils.command;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.NBTUtils;

public class CmdFly extends CmdBase {

    public CmdFly() {
        super("fly", Level.OP);
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

        if (nbt.getBoolean("fly")) {
            nbt.removeTag("fly");
            player.capabilities.allowFlying = false;
            player.capabilities.isFlying = false;
            sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.fly_off", name));
        } else {
            sender.addChatMessage(ServerUtilities.lang(sender, "serverutilities.lang.fly_on", name));
            nbt.setBoolean("fly", true);
            player.capabilities.allowFlying = true;
        }

        player.sendPlayerAbilities();
    }
}
