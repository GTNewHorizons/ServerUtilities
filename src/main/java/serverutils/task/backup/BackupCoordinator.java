package serverutils.task.backup;

import static serverutils.ServerUtilities.LOGGER;
import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import net.minecraft.util.EnumChatFormatting;

import serverutils.ServerUtilities;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.StringUtils;

public final class BackupCoordinator {

    private static final Object LOCK = BackupCoordinator.class;
    private static final long STOP_TIMEOUT_SECONDS = 30;

    private static RetentionRoutine pendingRoutine;
    private static boolean backupActive;
    private static boolean retentionActive;
    private static boolean stopping;

    private static BackupRoutine activeBackup;
    private static CompletableFuture<?> backupDone;
    private static RetentionRoutine activeRetention;
    private static CompletableFuture<?> retentionDone;

    private static final ExecutorService PRUNER = executor("ServerUtilities backup retention", Thread.NORM_PRIORITY);
    private static final ExecutorService BACKUP_EXECUTOR = executor("ServerUtilities backup", 7);

    public static boolean isRunning() {
        synchronized (LOCK) {
            return backupActive || retentionActive;
        }
    }

    public static boolean startBackup(BackupRoutine routine, Consumer<BackupResult> callback) {
        synchronized (LOCK) {
            if (isBackupBlocked()) return false;
            backupActive = true;
            activeBackup = routine;
        }
        CompletableFuture<BackupResult.Save> result;
        try {
            result = routine.save(BACKUP_EXECUTOR);
        } catch (RuntimeException e) {
            completeBackup(false);
            throw e;
        }
        synchronized (LOCK) {
            backupDone = result;
        }

        // The save future records failures on its result rather than completing exceptionally.
        result.thenCompose(save -> completeBackup(save.isSuccess()).handle((retention, error) -> {
            if (error != null) {
                LOGGER.error("Backup retention failed", error);
                retention = new BackupResult.Retention();
                retention.error = error;
                retention.finished();
            }
            return new BackupResult(save, retention);
        })).thenAccept(backup -> {
            notifyFinished(backup);
            if (callback != null) callback.accept(backup);
        }).exceptionally(t -> {
            LOGGER.error("Failed to report backup result", t);
            return null;
        });

        return true;
    }

    public static boolean startBackup(BackupRoutine routine) {
        return startBackup(routine, null);
    }

    /** Runs a one-shot retention job now, or fails the future if a backup or retention job is running. */
    public static <T> CompletableFuture<T> execute(RetentionRoutine routine, Job<T> function) {
        synchronized (LOCK) {
            if (isRetentionBlocked()) return rejected();
            return startRetention(routine, () -> function.run(routine));
        }
    }

    /** Cancels running backup and retention work and waits for it to finish its cleanup. */
    public static void stop() {
        CompletableFuture<?> backup;
        CompletableFuture<?> retention;
        synchronized (LOCK) {
            stopping = true;
            pendingRoutine = null;
            if (activeBackup != null) activeBackup.cancel();
            if (activeRetention != null) activeRetention.cancel();
            backup = backupDone;
            retention = retentionDone;
        }

        try {
            CompletableFuture.allOf(orDone(backup), orDone(retention)).get(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            ServerUtilities.LOGGER.warn("Timed out waiting for backup work to stop");
        } catch (ExecutionException | CancellationException e) {
            // The jobs report their own failures; all that matters here is that they are done.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (LOCK) {
                stopping = false;
            }
        }
    }

    private static CompletableFuture<BackupResult.Retention> runPendingPrune() {
        RetentionRoutine routine = pendingRoutine;
        if (routine == null || isRetentionBlocked()) return CompletableFuture.completedFuture(null);
        pendingRoutine = null;
        return startRetention(routine, routine::prune);
    }

    private static <T> CompletableFuture<T> startRetention(RetentionRoutine routine, Callable<T> work) {
        CompletableFuture<T> result = new CompletableFuture<>();
        retentionActive = true;
        activeRetention = routine;
        retentionDone = result;
        PRUNER.execute(() -> {
            T value = null;
            Throwable error = null;
            try {
                value = work.call();
            } catch (Throwable t) {
                error = t;
            }
            // Free the slot before completing the future, so a finished future means retention is idle again.
            completePrune();
            if (error == null) {
                result.complete(value);
            } else {
                result.completeExceptionally(error);
            }
        });
        return result;
    }

    private static <T> CompletableFuture<T> rejected() {
        CompletableFuture<T> result = new CompletableFuture<>();
        result.completeExceptionally(new RejectedExecutionException("Backup or retention scan is already running"));
        return result;
    }

    private static CompletableFuture<?> orDone(CompletableFuture<?> future) {
        return future == null ? CompletableFuture.completedFuture(null) : future;
    }

    private static void notifyFinished(BackupResult result) {
        BackupResult.Save save = result.save;
        if (!save.isSuccess()) return;
        String size = FileUtils.getSizeString(save.bytes);

        LOGGER.info("Backup done in {} ({})!", save.getDoneTime(), size);
        LOGGER.info("Created {} from {}", save.destination.getAbsolutePath(), save.world.getAbsolutePath());

        if (backups.display_file_size) {
            String sizeT = FileUtils.getSizeString(BackupRoutine.BACKUP_FOLDER);
            BACKUP.sendAll(
                    StringUtils.color(
                            "cmd.backup_end_2",
                            EnumChatFormatting.LIGHT_PURPLE,
                            save.getDoneTime(),
                            (size.equals(sizeT) ? size : (size + " | " + sizeT))));
        } else {
            BACKUP.sendAll(StringUtils.color("cmd.backup_end_1", EnumChatFormatting.LIGHT_PURPLE, save.getDoneTime()));
        }
    }

    private static boolean isBackupBlocked() {
        return backupActive || stopping;
    }

    private static boolean isRetentionBlocked() {
        return backupActive || retentionActive || stopping;
    }

    private static CompletableFuture<BackupResult.Retention> completeBackup(boolean success) {
        synchronized (LOCK) {
            backupActive = false;
            activeBackup = null;
            backupDone = null;
            if (success && pendingRoutine == null) pendingRoutine = RetentionRoutine.fromConfig();
            return runPendingPrune();
        }
    }

    private static void completePrune() {
        synchronized (LOCK) {
            retentionActive = false;
            activeRetention = null;
            retentionDone = null;
            runPendingPrune(); // rerun in case another prune was requested while running
        }
    }

    private static ExecutorService executor(String name, int priority) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, name);
            thread.setDaemon(true);
            thread.setPriority(priority);
            thread.setUncaughtExceptionHandler(
                    (t, e) -> { ServerUtilities.LOGGER.error("Uncaught exception in backup thread", e); });
            return thread;
        });
    }

    @FunctionalInterface
    public interface Job<T> {

        T run(RetentionRoutine routine) throws IOException;

    }
}
