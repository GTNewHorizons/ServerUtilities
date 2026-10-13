package serverutils.command;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.handlers.ServerUtilitiesServerEventHandler;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.data.Universe;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.compression.ICompress;
import serverutils.task.backup.BackupCoordinator;
import serverutils.task.backup.BackupEntry;
import serverutils.task.backup.BackupResult;
import serverutils.task.backup.BackupRetention;
import serverutils.task.backup.BackupRoutine;
import serverutils.task.backup.RetentionRoutine;

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
    private static IChatComponent when(BackupEntry entry, long now) {
        if (BackupRoutine.BACKUP_NAME_PATTERN.matcher(entry.id()).matches()) {
            return age(entry.createdAt(), now);
        }
        return ServerUtilities.lang("cmd.backup_when", date(entry.createdAt()), age(entry.createdAt(), now));
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
            replyAsync(
                    sender,
                    CmdBackupPrune::previewRetentionAsync,
                    (plan, error) -> sendPreview(sender, plan, error));
        }

        private static CompletableFuture<BackupRetention.Plan<? extends BackupEntry>> previewRetentionAsync() {
            return BackupCoordinator.execute(RetentionRoutine.fromConfig(), RetentionRoutine::preview);
        }

        private static <T extends BackupEntry> void sendPreview(ICommandSender sender,
                @Nullable BackupRetention.Plan<T> plan, @Nullable Throwable error) {
            if (error != null) {
                sender.addChatMessage(ServerUtilities.lang("cmd.backup_prune_error", error.getMessage()));
                return;
            }

            if (plan == null) {
                sender.addChatMessage(ServerUtilities.lang("cmd.backup_prune_legacy_active"));
                return;
            }

            long now = System.currentTimeMillis();
            long deletedSize = 0;
            sender.addChatMessage(ServerUtilities.lang("cmd.backup_prune_header"));
            for (T entry : plan.entries()) {
                boolean delete = plan.delete.containsKey(entry);
                if (delete) deletedSize += entry.size();
                IChatComponent status = ServerUtilities
                        .lang(delete ? "cmd.backup_prune_delete" : "cmd.backup_prune_keep");
                status.getChatStyle().setColor(
                        delete ? EnumChatFormatting.RED
                                : plan.isPreserved(entry) ? EnumChatFormatting.YELLOW : EnumChatFormatting.GREEN);
                long until = plan.keptUntil(entry);
                String[] label = plan.label(entry);
                IChatComponent reason = ServerUtilities
                        .lang(label[0], (Object[]) Arrays.copyOfRange(label, 1, label.length));
                if (until >= 0) reason = ServerUtilities.lang("cmd.backup_prune_until", reason, date(until));
                sender.addChatMessage(
                        ServerUtilities.lang(
                                "cmd.backup_prune_entry",
                                status,
                                entry.id(),
                                FileUtils.getSizeString(entry.size()),
                                when(entry, now),
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
            final boolean oc = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("=oc"));
            final boolean overwrite = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("=overwrite"));
            final String target = Arrays.stream(args)
                    .filter(arg -> !arg.equalsIgnoreCase("=oc") && !arg.equalsIgnoreCase("=overwrite")).findFirst()
                    .orElse("");

            BackupRoutine.Options options = BackupRoutine.Options.fromConfig();
            options.overwrite = overwrite;
            options.customName = target.isEmpty() ? null : target;
            options.onlyClaimed |= oc;

            ICompress compressor = ICompress.createCompressor();
            BackupRoutine routine = new BackupRoutine(Universe.get(), compressor, options);

            try {
                routine.verifyBackupName();
            } catch (IllegalArgumentException e) {
                throw new WrongUsageException(e.getMessage());
            }

            if (!BackupCoordinator.startBackup(routine, result -> outputResult(sender, routine, result))) {
                sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_already_running"));
            } else if (routine.isPrepared()) {
                sender.addChatMessage(
                        ServerUtilities.lang(
                                sender,
                                "cmd.backup_manual_launch" + (oc ? "_oc" : ""),
                                sender.getCommandSenderName()));
            }
        }

        private void outputResult(ICommandSender sender, BackupRoutine routine, BackupResult result) {
            if (!result.isSuccess()) {
                // Archive failures are broadcast as cmd.backup_fail and cancellations are quiet, so only a backup
                // that never started gets this.
                if (!routine.isPrepared()) {
                    sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_manual_failed"));
                }
                return;
            }

            // only incremental reports the output in chat
            if (!result.save.isIncremental) return;

            BackupResult.Save saveResult = result.save;
            long bytesRemoved = result.retention == null ? 0 : result.retention.bytesRemoved;

            sender.addChatMessage(
                    new ChatComponentText(
                            String.format("Finished snapshot in %.1fs", saveResult.duration.toMillis() / 1000.0)));

            sender.addChatMessage(
                    new ChatComponentText(
                            String.format(
                                    "Total bytes: %s (+%s / -%s)",
                                    FileUtils.getSizeString(saveResult.bytes - bytesRemoved),
                                    FileUtils.getSizeString(saveResult.bytes),
                                    FileUtils.getSizeString(bytesRemoved))));

            sender.addChatMessage(
                    new ChatComponentText(
                            String.format(
                                    "%d new chunks, %d regions, %d chunks and %d files unchanged",
                                    saveResult.newFiles,
                                    saveResult.unmodifiedRegions,
                                    saveResult.unmodifiedChunks,
                                    saveResult.unmodifiedFiles)));
        }
    }

    public static class CmdBackupStop extends CmdBase {

        public CmdBackupStop(String s) {
            super(s, Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (BackupCoordinator.isRunning()) {
                BackupCoordinator.stop();
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
            String sizeT = FileUtils.getSizeString(BackupRoutine.BACKUP_FOLDER);
            sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_size", sizeW, sizeT));
        }
    }

    public static class CmdBackupList extends CmdBase {

        public CmdBackupList() {
            super("list", Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            replyAsync(sender, CmdBackupList::listBackupsAsync, (archives, error) -> sendList(sender, archives, error));
        }

        private static CompletableFuture<List<? extends BackupEntry>> listBackupsAsync() {
            return BackupCoordinator.execute(RetentionRoutine.fromConfig(), RetentionRoutine::list);
        }

        private static void sendList(ICommandSender sender, List<? extends BackupEntry> archives, Throwable error) {
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
                            FileUtils.getSizeString(archives.stream().mapToLong(BackupEntry::size).sum())));

            long now = System.currentTimeMillis();
            archives.stream().sorted(Comparator.comparingLong(BackupEntry::createdAt).thenComparing(BackupEntry::id))
                    .forEach(
                            archive -> sender.addChatMessage(
                                    ServerUtilities.lang(
                                            sender,
                                            "cmd.backup_list_file",
                                            archive.id(),
                                            FileUtils.getSizeString(archive.size()),
                                            when(archive, now))));
        }
    }
}
