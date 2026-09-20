package serverutils.command;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.handlers.ServerUtilitiesServerEventHandler;
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

    /** RCON executes commands on its own thread. Return the reply there, even if queued work finishes after timeout. */
    static IChatComponent runBackupCommand(boolean rcon, Supplier<IChatComponent> command) {
        if (!rcon) return command.get();
        FutureTask<IChatComponent> task = new FutureTask<>(command::get);
        ServerUtilitiesServerEventHandler.scheduleServerTask(task);
        try {
            return task.get(ServerUtilitiesConfig.backups.external_hold_prepare_timeout_seconds, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            ServerUtilities.LOGGER.error("Backup command failed on the server thread", ex.getCause());
        } catch (TimeoutException ex) {
            ServerUtilities.LOGGER.warn("Timed out waiting for backup command completion");
        }
        // Prevent work that has not started yet; an operation already running must finish its cleanup.
        task.cancel(false);
        return ServerUtilities.lang("cmd.backup_command_unconfirmed");
    }

    public static class CmdBackupStart extends CmdBase {

        public CmdBackupStart(String s) {
            super(s, Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            sender.addChatMessage(
                    runBackupCommand(
                            "net.minecraft.network.rcon.RConConsoleSource".equals(sender.getClass().getName()),
                            () -> run(sender, args)));
        }

        private IChatComponent run(ICommandSender sender, String[] args) {
            final boolean oc = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("=oc"));
            final String target = Arrays.stream(args).filter(arg -> !arg.equalsIgnoreCase("=oc")).findFirst()
                    .orElse("");

            final BackupTask task = new BackupTask(sender, target, oc);

            if (ExternalBackupHold.INSTANCE.isHeld()) {
                return ServerUtilities.lang(sender, "cmd.backup_hold_active");
            } else if (!BackupTask.isBackupRunning()) {
                task.execute(Universe.get());
                return ServerUtilities
                        .lang("cmd.backup_manual_launch" + (oc ? "_oc" : ""), sender.getCommandSenderName());
            } else {
                return ServerUtilities.lang(sender, "cmd.backup_already_running");
            }
        }
    }

    public static class CmdBackupStop extends CmdBase {

        public CmdBackupStop(String s) {
            super(s, Level.OP_OR_SP);
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            sender.addChatMessage(
                    runBackupCommand(
                            "net.minecraft.network.rcon.RConConsoleSource".equals(sender.getClass().getName()),
                            () -> run(sender)));
        }

        private IChatComponent run(ICommandSender sender) {
            if (ExternalBackupHold.INSTANCE.isHeld()) {
                HoldResponse response = ExternalBackupHold.INSTANCE
                        .forceRelease("released by " + sender.getCommandSenderName());
                return ServerUtilities.lang(
                        sender,
                        response.isOk() ? "cmd.backup_hold_released" : "cmd.backup_hold_release_unconfirmed");
            } else if (BackupTask.isBackupRunning()) {
                BackupTask.stopBackupThread();
                return ServerUtilities.lang(sender, "cmd.backup_stop");
            } else {
                return ServerUtilities.lang(sender, "cmd.backup_not_running");
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
