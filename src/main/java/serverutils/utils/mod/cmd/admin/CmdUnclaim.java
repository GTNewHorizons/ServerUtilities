package serverutils.utils.mod.cmd.admin;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;

import serverutils.lib.LMAccessToken;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;
import serverutils.utils.mod.ServerUtilsGuiHandler;

public class CmdUnclaim extends CommandLM {

    public CmdUnclaim() {
        super("unclaim", CommandLevel.OP);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
        NBTTagCompound data = new NBTTagCompound();
        data.setLong("T", LMAccessToken.generate(ep));
        ServerUtilsGuiHandler.instance.openGui(ep, ServerUtilsGuiHandler.ADMIN_CLAIMS, data);
        return null;
    }
}
