package serverutils.command;

import java.util.Arrays;

import net.minecraft.command.ICommandSender;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.data.Universe;
import serverutils.lib.util.FileUtils;
import serverutils.task.backup.BackupTask;
import serverutils.task.backup.ExternalBackupHold;
import serverutils.task.backup.HoldResponse;

public class CmdBackup extends CmdTreeBase {

    public CmdBackup() {
        super("backup");
        addSubcommand(new CmdBackupStart("start"));
        addSubcommand(new CmdBackupStop("stop"));
        addSubcommand(new CmdBackupGetSize("getsize"));
    }

    /** Called only during dedicated-server command registration. */
    public void addHoldCommands() {
        addSubcommand(new CmdBackupHold());
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

            if (ExternalBackupHold.INSTANCE.isHeld()) {
                sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_hold_active"));
            } else if (!BackupTask.isBackupRunning()) {
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
            if (ExternalBackupHold.INSTANCE.isHeld()) {
                HoldResponse response = ExternalBackupHold.INSTANCE
                        .forceRelease("released by " + sender.getCommandSenderName());
                sender.addChatMessage(
                        ServerUtilities.lang(
                                sender,
                                response.isOk() ? "cmd.backup_hold_released" : "cmd.backup_hold_release_unconfirmed"));
            } else if (BackupTask.isBackupRunning()) {
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
}
