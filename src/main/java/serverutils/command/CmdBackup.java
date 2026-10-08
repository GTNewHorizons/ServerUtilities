package serverutils.command;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.network.rcon.RConConsoleSource;

import serverutils.ServerUtilities;
import serverutils.handlers.ServerUtilitiesServerEventHandler;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.data.Universe;
import serverutils.lib.util.FileUtils;
import serverutils.task.backup.BackupRetention;
import serverutils.task.backup.BackupTask;
import serverutils.task.backup.ThreadBackup;

public class CmdBackup extends CmdTreeBase {

    public CmdBackup() {
        super("backup");
        addSubcommand(new CmdBackupStart("start"));
        addSubcommand(new CmdBackupStop("stop"));
        addSubcommand(new CmdBackupGetSize("getsize"));
        addSubcommand(new CmdBackupPrune());
    }

    public static class CmdBackupPrune extends CmdBase {

        public CmdBackupPrune() {
            super("prune", Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length != 1 || !args[0].equals("preview")) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            try {
                CompletableFuture<BackupRetention.Plan> preview = BackupTask.previewRetentionAsync();
                if (sender instanceof RConConsoleSource) {
                    // RCON collects the reply before returning from this command, on its own thread.
                    sendPreview(sender, preview.join(), null);
                } else {
                    preview.whenComplete(
                            (plan, error) -> ServerUtilitiesServerEventHandler
                                    .scheduleServerTask(() -> sendPreview(sender, plan, error)));
                }
            } catch (RuntimeException ex) {
                sendPreview(sender, null, ex);
            }
        }

        private static void sendPreview(ICommandSender sender, BackupRetention.Plan plan, Throwable error) {
            if (error != null) {
                if (error instanceof CompletionException && error.getCause() != null) error = error.getCause();
                sender.addChatMessage(ServerUtilities.lang("cmd.backup_prune_error", error.getMessage()));
                return;
            }
            for (Map.Entry<File, String> decision : plan.keep.entrySet()) {
                sender.addChatMessage(
                        ServerUtilities
                                .lang("cmd.backup_prune_keep", decision.getKey().getName(), decision.getValue()));
            }
            for (Map.Entry<File, String> decision : plan.delete.entrySet()) {
                sender.addChatMessage(
                        ServerUtilities
                                .lang("cmd.backup_prune_delete", decision.getKey().getName(), decision.getValue()));
            }
            sender.addChatMessage(
                    ServerUtilities.lang(
                            "cmd.backup_prune_summary",
                            plan.keep.size(),
                            plan.delete.size(),
                            plan.remainingSize,
                            plan.remainingRotationSize));
            if (serverutils.ServerUtilitiesConfig.backups.max_folder_size > 0 && plan.remainingRotationSize
                    > serverutils.ServerUtilitiesConfig.backups.max_folder_size * FileUtils.SizeUnit.GB.getSize()) {
                sender.addChatMessage(ServerUtilities.lang("cmd.backup_prune_limit"));
            }
        }
    }

    public static class CmdBackupStart extends CmdBase {

        public CmdBackupStart(String s) {
            super(s, Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws WrongUsageException {
            final boolean oc = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("=oc"));
            final boolean overwrite = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("=overwrite"));
            final String target = Arrays.stream(args)
                    .filter(arg -> !arg.equalsIgnoreCase("=oc") && !arg.equalsIgnoreCase("=overwrite")).findFirst()
                    .orElse("");
            try {
                ThreadBackup.validateBackupName(target);
                if (!target.isEmpty() || overwrite) ThreadBackup.backupDestination(target, overwrite);
            } catch (IOException ex) {
                throw new WrongUsageException(ex.getMessage());
            }

            final BackupTask task = new BackupTask(sender, target, oc, overwrite);

            if (!BackupTask.isBackupRunning()) {
                task.execute(Universe.get());
                if (task.isDeferred()) {
                    sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_already_running"));
                } else if (task.hasStarted()) sender.addChatMessage(
                        ServerUtilities
                                .lang("cmd.backup_manual_launch" + (oc ? "_oc" : ""), sender.getCommandSenderName()));
                else sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_manual_failed"));
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
}
