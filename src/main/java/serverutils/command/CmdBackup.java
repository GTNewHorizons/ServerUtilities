package serverutils.command;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.data.Universe;
import serverutils.lib.util.FileUtils;
import serverutils.task.backup.BackupTask;

public class CmdBackup extends CmdTreeBase {

    public CmdBackup() {
        super("backup");
        addSubcommand(new CmdBackupStart("start"));
        addSubcommand(new CmdBackupStop("stop"));
        addSubcommand(new CmdBackupGetSize("getsize"));
        addSubcommand(new CmdBackupList());
    }

    public static class CmdBackupStart extends CmdBase {

        public CmdBackupStart(String s) {
            super(s, Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            final boolean oc = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("=oc"));
            final String target = Arrays.stream(args).filter(arg -> !arg.equalsIgnoreCase("=oc")).findFirst()
                    .orElse("");

            final BackupTask task = new BackupTask(sender, target, oc);

            if (!BackupTask.isBackupRunning()) {
                task.execute(Universe.get());
                sender.addChatMessage(
                        ServerUtilities
                                .lang("cmd.backup_manual_launch" + (oc ? "_oc" : ""), sender.getCommandSenderName()));
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
            if (BackupTask.isBackupRunning()) {
                BackupTask.stopBackupThread();
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
            String sizeT = FileUtils.getSizeString(BackupTask.BACKUP_FOLDER);
            sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_size", sizeW, sizeT));
        }
    }

    public static class CmdBackupList extends CmdBase {

        private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        public CmdBackupList() {
            super("list", Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            File[] files = BackupTask.BACKUP_FOLDER.listFiles();
            if (files == null || files.length == 0) {
                sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_list_none"));
                return;
            }

            sender.addChatMessage(
                    ServerUtilities.lang(
                            sender,
                            "cmd.backup_list_header",
                            files.length,
                            FileUtils.getSizeString(BackupTask.BACKUP_FOLDER)));
            Arrays.stream(files).sorted(Comparator.comparingLong(File::lastModified)).forEach(
                    file -> sender.addChatMessage(
                            ServerUtilities.lang(
                                    sender,
                                    "cmd.backup_list_file",
                                    file.getName(),
                                    format.format(new Date(file.lastModified())),
                                    FileUtils.getSizeString(file))));
        }
    }
}
