package serverutils.command;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.handlers.ServerUtilitiesServerEventHandler;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.data.Universe;
import serverutils.lib.util.FileUtils;
import serverutils.task.backup.BackupRetention;
import serverutils.task.backup.BackupTask;

public class CmdBackup extends CmdTreeBase {

    public CmdBackup() {
        super("backup");
        addSubcommand(new CmdBackupStart("start"));
        addSubcommand(new CmdBackupStop("stop"));
        addSubcommand(new CmdBackupGetSize("getsize"));
        addSubcommand(new CmdBackupPrune());
        addSubcommand(new CmdBackupList());
    }

    private static String date(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(millis));
    }

    private static IChatComponent age(long created, long now) {
        if (created > now) return ServerUtilities.lang("cmd.backup_age_future");
        long seconds = (now - created) / 1000;
        if (seconds < 60) return ServerUtilities.lang("cmd.backup_age_seconds", seconds);
        if (seconds < 3600) return ServerUtilities.lang("cmd.backup_age_minutes", seconds / 60);
        if (seconds < 172_800) return ServerUtilities.lang("cmd.backup_age_hours", seconds / 3600);
        return ServerUtilities.lang("cmd.backup_age_days", seconds / 86_400);
    }

    /** Runs a background backup scan and replies on the server thread, or inline for RCON. */
    private static <T> void replyAsync(ICommandSender sender, Supplier<CompletableFuture<T>> scan,
            BiConsumer<T, Throwable> reply) {
        BiConsumer<T, Throwable> unwrapped = (result, error) -> reply.accept(
                result,
                error instanceof CompletionException && error.getCause() != null ? error.getCause() : error);
        try {
            CompletableFuture<T> future = scan.get();
            if (sender instanceof RConConsoleSource) {
                // RCON collects the reply before returning from this command, on its own thread.
                unwrapped.accept(future.join(), null);
            } else {
                future.whenComplete(
                        (result, error) -> ServerUtilitiesServerEventHandler
                                .scheduleServerTask(() -> unwrapped.accept(result, error)));
            }
        } catch (RuntimeException ex) {
            unwrapped.accept(null, ex);
        }
    }

    /** Timestamp names already show the date, so only custom names get it spelled out. */
    private static IChatComponent when(BackupRetention.Archive archive, long now) {
        if (BackupTask.BACKUP_NAME_PATTERN.matcher(archive.file.getName()).matches()) {
            return age(archive.created, now);
        }
        return ServerUtilities.lang("cmd.backup_when", date(archive.created), age(archive.created, now));
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
            replyAsync(sender, BackupTask::previewRetentionAsync, (plan, error) -> sendPreview(sender, plan, error));
        }

        private static void sendPreview(ICommandSender sender, BackupRetention.Plan plan, Throwable error) {
            if (error != null) {
                sender.addChatMessage(ServerUtilities.lang("cmd.backup_prune_error", error.getMessage()));
                return;
            }
            long now = System.currentTimeMillis();
            long deletedSize = 0;
            sender.addChatMessage(ServerUtilities.lang("cmd.backup_prune_header"));
            for (BackupRetention.Archive archive : plan.archives()) {
                File file = archive.file;
                boolean delete = plan.delete.containsKey(file);
                if (delete) deletedSize += archive.size;
                IChatComponent status = ServerUtilities
                        .lang(delete ? "cmd.backup_prune_delete" : "cmd.backup_prune_keep");
                status.getChatStyle().setColor(
                        delete ? EnumChatFormatting.RED
                                : plan.isPreserved(file) ? EnumChatFormatting.YELLOW : EnumChatFormatting.GREEN);
                long until = plan.keptUntil(file);
                String[] label = plan.label(file);
                IChatComponent reason = ServerUtilities
                        .lang(label[0], (Object[]) Arrays.copyOfRange(label, 1, label.length));
                if (until >= 0) reason = ServerUtilities.lang("cmd.backup_prune_until", reason, date(until));
                sender.addChatMessage(
                        ServerUtilities.lang(
                                "cmd.backup_prune_entry",
                                status,
                                file.getName(),
                                FileUtils.getSizeString(archive.size),
                                when(archive, now),
                                reason));
            }
            sender.addChatMessage(
                    plan.delete.isEmpty()
                            ? ServerUtilities.lang(
                                    "cmd.backup_prune_summary_none",
                                    plan.keep.size(),
                                    FileUtils.getSizeString(plan.remainingSize))
                            : ServerUtilities.lang(
                                    "cmd.backup_prune_summary",
                                    plan.keep.size(),
                                    FileUtils.getSizeString(plan.remainingSize),
                                    plan.delete.size(),
                                    FileUtils.getSizeString(deletedSize)));
            long allowance = ServerUtilitiesConfig.backups.max_folder_size * FileUtils.SizeUnit.GB.getSize();
            if (allowance <= 0) {
                sender.addChatMessage(ServerUtilities.lang("cmd.backup_prune_no_allowance"));
                return;
            }
            sender.addChatMessage(
                    ServerUtilities.lang(
                            "cmd.backup_prune_allowance",
                            FileUtils.getSizeString(plan.remainingRotationSize),
                            FileUtils.getSizeString(allowance)));
            if (plan.remainingRotationSize > allowance) {
                IChatComponent warning = ServerUtilities.lang("cmd.backup_prune_limit");
                warning.getChatStyle().setColor(EnumChatFormatting.YELLOW);
                sender.addChatMessage(warning);
            }
        }
    }

    public static class CmdBackupStart extends CmdBase {

        public CmdBackupStart(String s) {
            super(s, Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws WrongUsageException {
            BackupTask.startSnapshot(Universe.get(), sender);
            // final boolean oc = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("=oc"));
            // final boolean overwrite = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("=overwrite"));
            // final String target = Arrays.stream(args)
            // .filter(arg -> !arg.equalsIgnoreCase("=oc") && !arg.equalsIgnoreCase("=overwrite")).findFirst()
            // .orElse("");
            // try {
            // ThreadBackup.validateBackupName(target);
            // if (!target.isEmpty() || overwrite) ThreadBackup.backupDestination(target, overwrite);
            // } catch (IOException ex) {
            // throw new WrongUsageException(ex.getMessage());
            // }
            //
            // final BackupTask task = new BackupTask(sender, target, oc, overwrite);
            //
            // if (!BackupTask.isBackupRunning()) {
            // task.execute(Universe.get());
            // if (task.isDeferred()) {
            // sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_already_running"));
            // } else if (task.hasStarted()) sender.addChatMessage(
            // ServerUtilities
            // .lang("cmd.backup_manual_launch" + (oc ? "_oc" : ""), sender.getCommandSenderName()));
            // else sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_manual_failed"));
            // } else {
            // sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_already_running"));
            // }
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

        public CmdBackupList() {
            super("list", Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            replyAsync(sender, BackupTask::listBackupsAsync, (archives, error) -> sendList(sender, archives, error));
        }

        private static void sendList(ICommandSender sender, List<BackupRetention.Archive> archives, Throwable error) {
            if (error != null) {
                sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_list_error", error.getMessage()));
                return;
            }
            if (archives.isEmpty()) {
                sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_list_none"));
                return;
            }

            sender.addChatMessage(
                    ServerUtilities.lang(
                            sender,
                            "cmd.backup_list_header",
                            archives.size(),
                            FileUtils.getSizeString(archives.stream().mapToLong(archive -> archive.size).sum())));

            long now = System.currentTimeMillis();
            archives.stream()
                    .sorted(
                            Comparator.comparingLong((BackupRetention.Archive archive) -> archive.created)
                                    .thenComparing(archive -> archive.file.getName()))
                    .forEach(
                            archive -> sender.addChatMessage(
                                    ServerUtilities.lang(
                                            sender,
                                            "cmd.backup_list_file",
                                            archive.file.getName(),
                                            FileUtils.getSizeString(archive.size),
                                            when(archive, now))));
        }
    }
}
