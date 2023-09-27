package serverutils.utils.command;

import static serverutils.lib.lib.command.CmdBase.Level.OP;

import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CmdTreeBase;
import serverutils.lib.lib.util.BackupUtils;
import serverutils.lib.lib.util.BroadcastSender;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesConfig;
import serverutils.utils.backups.Backups;

public class CmdBackup extends CmdTreeBase {

    public CmdBackup() {
        super("backup");
        addSubcommand(new CmdBackupStart("start"));
        addSubcommand(new CmdBackupStop("stop"));
        addSubcommand(new CmdBackupGetSize("getsize"));
    }

    // @Override
    // public void processCommand(ICommandSender sender, String[] args) {
    //
    // }

    public static class CmdBackupStart extends CmdBase {

        public CmdBackupStart(String s) {
            super(s, OP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            boolean b = Backups.run(sender);
            if (b) {
                sender.addChatMessage(ServerUtilities.lang(BroadcastSender.inst, "cmd.backup_manual_launch"));
                if (!ServerUtilitiesConfig.backups.use_separate_thread) Backups.postBackup();
            } else {
                sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_already_running"));
            }
        }
    }

    public static class CmdBackupStop extends CmdBase {

        public CmdBackupStop(String s) {
            super(s, OP);
        }

        // public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        // if (Backups.thread != null) {
        // Backups.thread.interrupt();
        // Backups.thread = null;
        // return FTBU.mod.chatComponent("cmd.backup_stop");
        // }
        //
        // return error(FTBU.mod.chatComponent("cmd.backup_not_running"));
        // }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (Backups.thread != null) {
                Backups.thread.interrupt();
                Backups.thread = null;
                sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_stop"));
            } else {
                sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_not_running"));
            }
        }
    }

    public static class CmdBackupGetSize extends CmdBase {

        public CmdBackupGetSize(String s) {
            super(s, OP);
        }

        // public IChatComponent onCommand(ICommandSender ics, String[] args) throws CommandException {
        // String sizeW = BackupUtils.getSizeS(ics.getEntityWorld().getSaveHandler().getWorldDirectory());
        // String sizeT = BackupUtils.getSizeS(Backups.backupsFolder);
        // return FTBU.mod.chatComponent("cmd.backup_size", sizeW, sizeT);
        // }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            String sizeW = BackupUtils.getSizeS(sender.getEntityWorld().getSaveHandler().getWorldDirectory());
            String sizeT = BackupUtils.getSizeS(Backups.backupsFolder);
            sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_not_running", sizeW, sizeT));
        }
    }
}
