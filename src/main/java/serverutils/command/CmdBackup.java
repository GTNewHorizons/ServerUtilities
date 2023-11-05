package serverutils.command;

import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.backups.Backups;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.util.FileUtils;

public class CmdBackup extends CmdTreeBase {

    public CmdBackup() {
        super("backup");
        addSubcommand(new CmdBackupStart("start"));
        addSubcommand(new CmdBackupStop("stop"));
        addSubcommand(new CmdBackupGetSize("getsize"));
    }

    public static class CmdBackupStart extends CmdBase {

        public CmdBackupStart(String s) {
            super(s, Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            boolean b = Backups.run(sender, args.length == 0 ? "" : args[0]);
            if (b) {
                sender.addChatMessage(
                        ServerUtilities.lang(null, "cmd.backup_manual_launch", sender.getCommandSenderName()));
                if (!ServerUtilitiesConfig.backups.use_separate_thread) Backups.postBackup();
            } else {
                sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_already_running"));
            }
        }
    }

    public static class CmdBackupStop extends CmdBase {

        public CmdBackupStop(String s) {
            super(s, Level.OP_OR_SP);
        }

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
            super(s, Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            String sizeW = FileUtils.getSizeString(sender.getEntityWorld().getSaveHandler().getWorldDirectory());
            String sizeT = FileUtils.getSizeString(Backups.backupsFolder);
            sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_not_running", sizeW, sizeT));
        }
    }
}
