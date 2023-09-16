package serverutils.utils.mod.cmd.admin;

import net.minecraft.command.*;
import net.minecraft.util.IChatComponent;

import latmod.lib.LMFileUtils;
import serverutils.lib.*;
import serverutils.lib.api.cmd.*;
import serverutils.utils.mod.ServerUtilities;
import serverutils.utils.mod.config.ServerUtilitiesConfigBackups;
import serverutils.utils.world.Backups;

public class CmdBackup extends CommandSubLM {

    public CmdBackup() {
        super("backup", CommandLevel.OP);
        add(new CmdBackupStart("start"));
        add(new CmdBackupStop("stop"));
        add(new CmdBackupGetSize("getsize"));
    }

    public static class CmdBackupStart extends CommandLM {

        public CmdBackupStart(String s) {
            super(s, CommandLevel.OP);
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            boolean b = Backups.run(ics);
            if (b) {
                ServerUtilitiesLib.printChat(
                        BroadcastSender.inst,
                        ServerUtilities.mod.chatComponent("cmd.backup_manual_launch", ics.getCommandSenderName()));
                if (!ServerUtilitiesConfigBackups.use_separate_thread.getAsBoolean()) Backups.postBackup();
            }
            return b ? null : error(ServerUtilities.mod.chatComponent("cmd.backup_already_running"));
        }
    }

    public static class CmdBackupStop extends CommandLM {

        public CmdBackupStop(String s) {
            super(s, CommandLevel.OP);
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            if (Backups.thread != null) {
                Backups.thread.interrupt();
                Backups.thread = null;
                return ServerUtilities.mod.chatComponent("cmd.backup_stop");
            }

            return error(ServerUtilities.mod.chatComponent("cmd.backup_not_running"));
        }
    }

    public static class CmdBackupGetSize extends CommandLM {

        public CmdBackupGetSize(String s) {
            super(s, CommandLevel.OP);
        }

        public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
            String sizeW = LMFileUtils.getSizeS(ics.getEntityWorld().getSaveHandler().getWorldDirectory());
            String sizeT = LMFileUtils.getSizeS(Backups.backupsFolder);
            return ServerUtilities.mod.chatComponent("cmd.backup_size", sizeW, sizeT);
        }
    }
}
