package ftb.utils.mod.cmd.admin;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;

import ftb.lib.LMAccessToken;
import ftb.lib.api.cmd.*;
import ftb.utils.mod.FTBUGuiHandler;

public class CmdUnclaim extends CommandLM {

    public CmdUnclaim() {
        super("unclaim", CommandLevel.OP);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        EntityPlayerMP ep = getCommandSenderAsPlayer(ics);
        NBTTagCompound data = new NBTTagCompound();
        data.setLong("T", LMAccessToken.generate(ep));
        FTBUGuiHandler.instance.openGui(ep, FTBUGuiHandler.ADMIN_CLAIMS, data);
        return null;
    }
}
