package serverutils.command;

import cpw.mods.fml.common.FMLCommonHandler;
import java.io.File;
import java.io.IOException;
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
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import net.minecraftforge.common.DimensionManager;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.handlers.ServerUtilitiesServerEventHandler;
import serverutils.lib.command.CmdBase;
import serverutils.lib.command.CmdTreeBase;
import serverutils.lib.data.Universe;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.backup.Snapshot;
import serverutils.lib.util.backup.SnapshotRestorer;
import serverutils.lib.util.backup.SnapshotStore;
import serverutils.lib.util.compression.ICompress;
import serverutils.task.backup.BackupCoordinator;
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
        addSubcommand(new CmdBackupRestore());
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
        if (BackupRoutine.BACKUP_NAME_PATTERN.matcher(archive.file.getName()).matches()) {
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
            replyAsync(
                    sender,
                    CmdBackupPrune::previewRetentionAsync,
                    (plan, error) -> sendPreview(sender, plan, error));
        }

        private static CompletableFuture<BackupRetention.Plan> previewRetentionAsync() {
            return BackupCoordinator.execute(RetentionRoutine.fromConfig(), RetentionRoutine::preview);
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

    public static class CmdBackupRestore extends CmdBase {
        public CmdBackupRestore() {
            // this command creates entire new worlds and thus should only be allowed by the server
            super("restore", Level.SERVER);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            String backupName = args[0];

            try {
                SnapshotStore snapshotStore = SnapshotStore.loadFromSession();
                Snapshot snapshot = snapshotStore.find(backupName);
                if (snapshot == null) {
                    sender.addChatMessage(new ChatComponentText("Backup not found"));
                    return;
                }

                File currentSave = DimensionManager.getCurrentSaveRootDirectory();
                File savesFolder = FMLCommonHandler.instance().getSavesDirectory();
                File backupSave = FileUtils.getFileWithoutCollision(savesFolder, currentSave.getName() + "-command-restored", Object::toString);
                SnapshotRestorer.restore(snapshotStore, snapshot, backupSave);
            } catch (IOException e) {
                throw new RuntimeException(e);
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

            ICompress compressor = ICompress.createCompressor();
            BackupRoutine routine = new BackupRoutine(Universe.get(), compressor, options);

            try {
                routine.verifyBackupName();
            } catch (IllegalArgumentException e) {
                throw new WrongUsageException(e.getMessage());
            }

            if (!BackupCoordinator.startBackup(routine)) {
                sender.addChatMessage(ServerUtilities.lang(sender, "cmd.backup_already_running"));
            } else {
                sender.addChatMessage(
                        ServerUtilities.lang(
                                sender,
                                "cmd.backup_manual_launch" + (oc ? "_oc" : ""),
                                sender.getCommandSenderName()));
            }
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

        private static CompletableFuture<List<BackupRetention.Archive>> listBackupsAsync() {
            return BackupCoordinator.execute(RetentionRoutine.fromConfig(), RetentionRoutine::list);
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
