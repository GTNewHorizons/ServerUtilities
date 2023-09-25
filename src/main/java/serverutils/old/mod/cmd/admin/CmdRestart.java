package serverutils.old.mod.cmd.admin;

import java.io.File;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;

import latmod.lib.LMFileUtils;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.api.cmd.CommandLM;
import serverutils.lib.api.cmd.CommandLevel;

public class CmdRestart extends CommandLM {

    public CmdRestart() {
        super("restart", CommandLevel.OP);
    }

    public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        restart();
        return null;
    }

    public static void restart() {
        LMFileUtils.newFile(new File(ServerUtilitiesLib.folderMinecraft, "autostart.stamp"));
        ServerUtilitiesLib.getServer().initiateShutdown();
    }
}
