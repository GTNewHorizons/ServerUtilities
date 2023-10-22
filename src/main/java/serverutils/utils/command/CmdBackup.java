package serverutils.utils.command;

import static serverutils.lib.lib.command.CmdBase.Level.OP;

import net.minecraft.command.ICommandSender;

import serverutils.lib.lib.command.CmdBase;
import serverutils.lib.lib.command.CmdTreeBase;
import serverutils.lib.lib.util.FileUtils;
import serverutils.mod.ServerUtilities;
import serverutils.mod.ServerUtilitiesConfig;
import serverutils.utils.backups.Backups;

public class CmdBackup extends CmdTreeBase {

    public CmdBackup() {
        super("backup");
        addSubcommand(new CmdBackupStart("start"));
        addSubcommand(new CmdBackupStop("stop"));
        addSubcommand(new CmdBackupGetSize("getsize"));
    }

    public static class CmdBackupStart extends CmdBase {

        public CmdBackupStart(String s) {
            super(s, OP);
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
            super(s, OP);
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
            super(s, OP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            String sizeW = FileUtils.getSizeString(sender.getEntityWorld().getSaveHandler().getWorldDirectory());
            String sizeT = FileUtils.getSizeString(Backups.backupsFolder);
            sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_not_running", sizeW, sizeT));
        }
    }
}
