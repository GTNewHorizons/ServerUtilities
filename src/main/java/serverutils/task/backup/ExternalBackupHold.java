package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;

import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import serverutils.ServerUtilities;
import serverutils.handlers.ServerUtilitiesServerEventHandler;

/**
 * Lets an external backup tool suspend world saving while it snapshots or copies the server files, and resumes saving
 * on its own if that tool dies. See docs/external-backup-plan.md.
 * <p>
 * Commands arrive on the RCON thread, which in 1.7.10 executes them inline rather than queueing them to the server
 * thread, so every entry point here blocks until the server thread has answered. Only world mutation is dispatched; the
 * drain runs on the caller because a suspended world cannot change underneath it.
 * <p>
 * The safety property that matters is that world saving is never left suspended with nothing watching it. That is why
 * {@link #savingSuspended} is tracked separately from {@link #state}: a release whose dispatch never confirms leaves
 * the flag set, and {@link #tick()} retries it every tick until saving is genuinely back on.
 */
public final class ExternalBackupHold {

    public static final ExternalBackupHold INSTANCE = new ExternalBackupHold();

    /** Server-thread work, separated so tests can inject failures without a world. */
    interface Backend {

        void saveAndSuspend() throws Exception;

        void resume();

        void drain() throws Exception;

        /** Whether a normal backup already owns world saving. Answered on the server thread, where it cannot change. */
        boolean isBusy();
    }

    private enum State {
        IDLE,
        PREPARING,
        HELD
    }

    private static final Backend LIVE_BACKEND = new Backend() {

        @Override
        public void saveAndSuspend() throws Exception {
            BackupTask.saveAndSuspendForSnapshot(MinecraftServer.getServer());
        }

        @Override
        public void resume() {
            BackupTask.restoreWorldSaving();
        }

        @Override
        public void drain() throws Exception {
            BackupTask.drainQueuedWrites();
        }

        @Override
        public boolean isBusy() {
            return BackupTask.isBackupRunning() || BackupTask.isWorldSavingSuspended();
        }
    };

    private final SecureRandom random = new SecureRandom();
    private final Object lock = new Object();

    private State state = State.IDLE;
    private String token;
    private long deadlineNanos;
    private long nextWarnNanos;
    private long startedNanos;
    /** Bumped on every transition so a task that outlived its request cannot act on a newer hold. */
    private long generation;
    /** True whenever the backend has saving suspended for us, independent of state. Cleared only by a real resume. */
    private boolean savingSuspended;

    // Seams. Package-private so tests can drive time and failure without sleeping or booting a server. Volatile
    // because tests install them from one thread and the dispatched tasks read them from another.
    volatile LongSupplier clock = System::nanoTime;
    volatile Backend backend = LIVE_BACKEND;
    volatile Consumer<Runnable> dispatcher = ServerUtilitiesServerEventHandler::scheduleServerTask;

    private ExternalBackupHold() {}

    public static boolean isConsoleOrRcon(ICommandSender sender) {
        if (sender instanceof MinecraftServer) return true;
        // RConConsoleSource is SideOnly(SERVER), so it cannot be referenced directly from common code.
        return "net.minecraft.network.rcon.RConConsoleSource".equals(sender.getClass().getName());
    }

    /**
     * True while an external tool owns world saving, or while a release is still outstanding. Backups must not run and
     * nothing else may resume saving while this holds.
     */
    public boolean isHeld() {
        synchronized (lock) {
            return state != State.IDLE || savingSuspended;
        }
    }

    public HoldResponse begin(int requestedSeconds) {
        if (!backups.enable_external_holds) return HoldResponse.of(HoldResult.DISABLED);

        int requested = requestedSeconds > 0 ? requestedSeconds : backups.external_hold_default_seconds;
        int seconds = Math.min(requested, backups.external_hold_max_seconds);

        final long myGeneration;
        synchronized (lock) {
            if (state != State.IDLE || savingSuspended) return HoldResponse.of(HoldResult.BUSY);
            state = State.PREPARING;
            myGeneration = ++generation;
        }

        HoldResult failure = prepare(myGeneration, seconds);
        if (failure != null) return HoldResponse.of(failure);

        synchronized (lock) {
            if (generation != myGeneration) return HoldResponse.of(HoldResult.EXPIRED);
            return HoldResponse.granted(token, remainingSeconds(clock.getAsLong()), seconds < requested);
        }
    }

    /**
     * Saves and suspends on the server thread, then drains on this thread. Returns null on success, or the result to
     * report. Every failure path hands the suspension back for release.
     */
    private HoldResult prepare(long myGeneration, int seconds) {
        AtomicReference<Throwable> saveFailure = new AtomicReference<>();
        AtomicBoolean busy = new AtomicBoolean();
        CountDownLatch saved = new CountDownLatch(1);

        dispatcher.accept(() -> {
            try {
                synchronized (lock) {
                    // The request was abandoned while this sat in the queue; leave the world alone.
                    if (generation != myGeneration) return;
                }
                // Checked here rather than in begin() because this runs on the server thread, the same thread that
                // starts internal backups, so the answer cannot change between the check and the suspend.
                if (backend.isBusy()) {
                    busy.set(true);
                    return;
                }
                backend.saveAndSuspend();
                synchronized (lock) {
                    savingSuspended = true;
                }
            } catch (Throwable ex) {
                // Throwable, not Exception: an Error escaping here would otherwise look exactly like success and
                // hand out a hold over a world that was never suspended.
                saveFailure.set(ex);
            } finally {
                saved.countDown();
            }
        });

        try {
            if (!saved.await(backups.external_hold_prepare_timeout_seconds, TimeUnit.SECONDS)) {
                // The task may still be queued and may still suspend; abandon() leaves the resume to the watchdog.
                abandon(myGeneration);
                return HoldResult.TIMEOUT;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            abandon(myGeneration);
            return HoldResult.TIMEOUT;
        }

        if (saveFailure.get() != null) {
            ServerUtilities.LOGGER.error("External backup hold could not save the world", saveFailure.get());
            abandon(myGeneration);
            return HoldResult.SAVE_FAILED;
        }
        if (busy.get()) {
            abandon(myGeneration);
            return HoldResult.BUSY;
        }

        try {
            backend.drain();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            abandon(myGeneration);
            return HoldResult.SAVE_FAILED;
        } catch (Throwable ex) {
            // Hodgepodge's threaded world data saving reports write failures here rather than silently losing them.
            ServerUtilities.LOGGER.error("External backup hold could not flush world data", ex);
            abandon(myGeneration);
            return HoldResult.SAVE_FAILED;
        }

        // Generated outside the lock: seeding SecureRandom can block, and the server thread needs this lock every tick.
        String freshToken = newToken();
        long now = clock.getAsLong();
        synchronized (lock) {
            if (generation != myGeneration) return HoldResult.EXPIRED;
            if (!savingSuspended) {
                // Nothing confirmed the suspension, so there is no consistent point to hand out.
                clearLocked();
                return HoldResult.SAVE_FAILED;
            }
            state = State.HELD;
            token = freshToken;
            startedNanos = now;
            deadlineNanos = now + TimeUnit.SECONDS.toNanos(seconds);
            nextWarnNanos = now + TimeUnit.SECONDS.toNanos(backups.external_hold_warn_seconds);
        }
        ServerUtilities.LOGGER.info("External backup hold granted for {} seconds", seconds);
        return null;
    }

    public HoldResponse renew(String suppliedToken, int requestedSeconds) {
        int requested = requestedSeconds > 0 ? requestedSeconds : backups.external_hold_default_seconds;
        int seconds = Math.min(requested, backups.external_hold_max_seconds);

        long now = clock.getAsLong();
        synchronized (lock) {
            HoldResult invalid = validate(suppliedToken, now);
            if (invalid != null) return HoldResponse.of(invalid);

            deadlineNanos = now + TimeUnit.SECONDS.toNanos(seconds);
            return HoldResponse.granted(token, remainingSeconds(now), seconds < requested);
        }
    }

    public HoldResponse end(String suppliedToken) {
        long now = clock.getAsLong();
        final long myGeneration;
        synchronized (lock) {
            HoldResult invalid = validate(suppliedToken, now);
            if (invalid != null) return HoldResponse.of(invalid);
            // Carried forward so the release cannot clear a different hold granted while it was in flight.
            myGeneration = generation;
        }

        // Only report release once saving is actually back on, so a client that sees OK knows the server is writing.
        if (!release(myGeneration)) return HoldResponse.of(HoldResult.TIMEOUT);
        ServerUtilities.LOGGER.info("External backup hold released");
        return HoldResponse.of(HoldResult.OK);
    }

    public HoldResponse status() {
        if (!backups.enable_external_holds) return HoldResponse.of(HoldResult.DISABLED);
        long now = clock.getAsLong();
        synchronized (lock) {
            // A hold is being prepared, or a release has not confirmed yet; either way no token exists to report.
            if (state == State.PREPARING || savingSuspended && state == State.IDLE) {
                return HoldResponse.of(HoldResult.BUSY);
            }
            if (state == State.IDLE) return HoldResponse.of(HoldResult.NO_HOLD);
            if (now >= deadlineNanos) return HoldResponse.of(HoldResult.EXPIRED);
            return HoldResponse.granted(token, remainingSeconds(now), false);
        }
    }

    /**
     * Server thread. Expires a hold whose client stopped renewing, and retries a release that never confirmed. This is
     * the only thing standing between a dead backup script and a server that silently stops saving.
     */
    public void tick() {
        boolean resume = false;
        synchronized (lock) {
            if (state == State.IDLE) {
                // A release whose dispatch was dropped or timed out. Nothing else is watching this.
                resume = savingSuspended;
            } else if (state == State.HELD) {
                long now = clock.getAsLong();
                if (now >= deadlineNanos) {
                    ServerUtilities.LOGGER.warn(
                            "External backup hold expired without being released; resuming world saving. "
                                    + "Any backup taken from it is incomplete and must be discarded.");
                    clearLocked();
                    resume = true;
                } else if (now >= nextWarnNanos) {
                    ServerUtilities.LOGGER.warn(
                            "External backup hold has held world saving for {} seconds",
                            TimeUnit.NANOSECONDS.toSeconds(now - startedNanos));
                    nextWarnNanos = now + TimeUnit.SECONDS.toNanos(backups.external_hold_warn_seconds);
                }
            }
        }
        if (resume) resumeNow();
    }

    /**
     * Releases through the server thread and waits for it. For callers that are not the server thread, such as an
     * administrator running the stop command over RCON.
     */
    public HoldResponse forceRelease(String reason) {
        final long myGeneration;
        synchronized (lock) {
            if (state == State.IDLE && !savingSuspended) return HoldResponse.of(HoldResult.NO_HOLD);
            myGeneration = generation;
        }
        boolean confirmed = release(myGeneration);
        ServerUtilities.LOGGER.warn("External backup hold released: {}", reason);
        return HoldResponse.of(confirmed ? HoldResult.OK : HoldResult.TIMEOUT);
    }

    /**
     * Releases inline. Server thread only, for shutdown: the tick loop has stopped by then, so a dispatched release
     * would never run.
     */
    public void forceReleaseNow(String reason) {
        boolean had;
        synchronized (lock) {
            had = state != State.IDLE || savingSuspended;
            if (state != State.IDLE) clearLocked();
        }
        if (!had) return;
        resumeNow();
        ServerUtilities.LOGGER.warn("External backup hold released: {}", reason);
    }

    private HoldResult validate(String suppliedToken, long now) {
        if (!backups.enable_external_holds) return HoldResult.DISABLED;
        // The caller supplied a token, so it believes it holds a lease. If nothing is held, the watchdog, an admin or
        // shutdown already took it away; that is EXPIRED, not NO_HOLD, which is reserved for status on an idle server.
        if (state != State.HELD) return HoldResult.EXPIRED;
        if (now >= deadlineNanos) return HoldResult.EXPIRED;
        // A token from a hold that already ended means another may have started since; the caller must discard either
        // way, so this is reported the same as expiry.
        if (token == null || !token.equals(suppliedToken)) return HoldResult.BAD_TOKEN;
        return null;
    }

    private void abandon(long myGeneration) {
        synchronized (lock) {
            if (generation != myGeneration) return;
            clearLocked();
        }
        // The dispatched task may have suspended, or may still be queued and about to. Ask for a resume either way;
        // savingSuspended keeps tick() retrying until one lands.
        dispatchResume();
    }

    /**
     * Resumes saving through the server thread, then retires the hold. Returns false if the server thread never
     * confirmed, in which case the hold is left for the watchdog rather than being silently dropped.
     */
    private boolean release(long myGeneration) {
        boolean confirmed = dispatchResume();
        synchronized (lock) {
            if (generation != myGeneration) return confirmed;
            if (confirmed) {
                clearLocked();
            } else if (state == State.HELD) {
                // Bring the deadline forward so the watchdog retries the resume on the next tick.
                deadlineNanos = clock.getAsLong();
            }
        }
        return confirmed;
    }

    private boolean dispatchResume() {
        CountDownLatch released = new CountDownLatch(1);
        dispatcher.accept(() -> {
            try {
                resumeNow();
            } finally {
                released.countDown();
            }
        });
        try {
            return released.await(backups.external_hold_prepare_timeout_seconds, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** Server thread only. */
    private void resumeNow() {
        backend.resume();
        synchronized (lock) {
            savingSuspended = false;
        }
    }

    private void clearLocked() {
        state = State.IDLE;
        token = null;
        deadlineNanos = 0L;
        nextWarnNanos = 0L;
        startedNanos = 0L;
        generation++;
    }

    private long remainingSeconds(long now) {
        long remaining = deadlineNanos - now;
        return remaining <= 0 ? 0 : TimeUnit.NANOSECONDS.toSeconds(remaining);
    }

    private String newToken() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }

    /** Test seam: drops any hold and restores the live collaborators. */
    void resetForTests() {
        synchronized (lock) {
            clearLocked();
            savingSuspended = false;
            generation = 0L;
        }
        clock = System::nanoTime;
        backend = LIVE_BACKEND;
        dispatcher = ServerUtilitiesServerEventHandler::scheduleServerTask;
    }
}
