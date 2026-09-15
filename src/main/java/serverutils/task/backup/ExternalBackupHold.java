package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import serverutils.ServerUtilities;
import serverutils.handlers.ServerUtilitiesServerEventHandler;
import serverutils.ranks.Ranks;

/** External backup lease. World mutation runs on the server thread; queued I/O drains on a bounded worker. */
public final class ExternalBackupHold {

    public static final ExternalBackupHold INSTANCE = new ExternalBackupHold();

    /** World operations, replaceable in tests. */
    interface Backend {

        void saveAndSuspend(Runnable beforeWorldSave) throws Exception;

        void resume();

        void drain() throws Exception;

        /** Whether a normal backup already owns world saving. Answered on the server thread, where it cannot change. */
        boolean isBusy();
    }

    private enum State {
        IDLE,
        PREPARING,
        HELD,
        RELEASING
    }

    private static final Backend LIVE_BACKEND = new Backend() {

        @Override
        public void saveAndSuspend(Runnable beforeWorldSave) throws Exception {
            BackupTask.saveAndSuspendForSnapshot(MinecraftServer.getServer(), beforeWorldSave);
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
    /** Bumped on every transition to identify stale requests. */
    private long generation;
    /** Tracks a possible partial suspension separately from the lease state. */
    private boolean savingSuspended;
    private FutureTask<Void> pendingDrain;
    private Ranks deferredRanks;
    private final Map<UUID, Runnable> deferredPlayerWrites = new LinkedHashMap<>();
    private long nextDeferredRetryNanos;
    private volatile Thread serverThread;

    // Test seams read by dispatched tasks on another thread.
    volatile LongSupplier clock = System::nanoTime;
    volatile Backend backend = LIVE_BACKEND;
    volatile Consumer<Runnable> dispatcher = ServerUtilitiesServerEventHandler::scheduleServerTask;

    private ExternalBackupHold() {}

    public static boolean isConsoleOrRcon(ICommandSender sender) {
        if (sender instanceof MinecraftServer) return true;
        // RConConsoleSource is SideOnly(SERVER), so it cannot be referenced directly from common code.
        return "net.minecraft.network.rcon.RConConsoleSource".equals(sender.getClass().getName());
    }

    /** Blocks backup and pause while a lease, drain, or deferred save remains. */
    public boolean isHeld() {
        synchronized (lock) {
            return state != State.IDLE || savingSuspended
                    || pendingDrain != null && !pendingDrain.isDone()
                    || !deferredPlayerWrites.isEmpty()
                    || deferredRanks != null;
        }
    }

    /** Defer a player save, including stats, until world saving resumes. */
    public boolean deferPlayerWrite(UUID playerId, Runnable write) {
        synchronized (lock) {
            if (!savingSuspended) return false;
            deferredPlayerWrites.put(playerId, write);
            return true;
        }
    }

    public boolean hasDeferredPlayerWrite(UUID playerId) {
        synchronized (lock) {
            return deferredPlayerWrites.containsKey(playerId);
        }
    }

    public boolean deferRankSave(Ranks ranks) {
        synchronized (lock) {
            if (state == State.IDLE && !savingSuspended) return false;
            deferredRanks = ranks;
            return true;
        }
    }

    public HoldResponse begin(int requestedSeconds) {
        if (!backups.enable_external_holds) return HoldResponse.of(HoldResult.DISABLED);

        int requested = requestedSeconds > 0 ? requestedSeconds : backups.external_hold_default_seconds;
        int seconds = Math.min(requested, backups.external_hold_max_seconds);

        final long myGeneration;
        final long prepareDeadline;
        synchronized (lock) {
            if (state != State.IDLE || savingSuspended
                    || pendingDrain != null && !pendingDrain.isDone()
                    || !deferredPlayerWrites.isEmpty()
                    || deferredRanks != null) {
                return HoldResponse.of(HoldResult.BUSY);
            }
            state = State.PREPARING;
            myGeneration = ++generation;
            prepareDeadline = clock.getAsLong()
                    + TimeUnit.SECONDS.toNanos(backups.external_hold_prepare_timeout_seconds);
            deadlineNanos = prepareDeadline;
        }

        HoldResult failure = prepare(myGeneration, seconds, prepareDeadline);
        if (failure != null) return HoldResponse.of(failure);

        synchronized (lock) {
            if (generation != myGeneration) return HoldResponse.of(HoldResult.EXPIRED);
            return HoldResponse.granted(token, remainingSeconds(clock.getAsLong()), seconds < requested);
        }
    }

    /** Saves on the server thread and drains on a bounded worker. Returns null on success. */
    private HoldResult prepare(long myGeneration, int seconds, long prepareDeadline) {
        AtomicReference<Throwable> saveFailure = new AtomicReference<>();
        AtomicBoolean busy = new AtomicBoolean();
        CountDownLatch saved = new CountDownLatch(1);

        Runnable save = () -> {
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
                backend.saveAndSuspend(() -> {
                    synchronized (lock) {
                        savingSuspended = true;
                    }
                });
            } catch (Throwable ex) {
                // Throwable, not Exception: an Error escaping here would otherwise look exactly like success and
                // hand out a hold over a world that was never suspended.
                saveFailure.set(ex);
            } finally {
                saved.countDown();
            }
        };
        if (Thread.currentThread() == serverThread) save.run();
        else dispatcher.accept(save);

        try {
            long remaining = prepareDeadline - clock.getAsLong();
            if (remaining <= 0 || !saved.await(remaining, TimeUnit.NANOSECONDS)) {
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

        FutureTask<Void> drain = new FutureTask<>(() -> {
            backend.drain();
            return null;
        });
        synchronized (lock) {
            pendingDrain = drain;
        }
        Thread drainThread = new Thread(drain, "ServerUtilities backup hold drain");
        drainThread.setDaemon(true);
        drainThread.start();
        try {
            long remaining = prepareDeadline - clock.getAsLong();
            if (remaining <= 0) throw new TimeoutException();
            drain.get(remaining, TimeUnit.NANOSECONDS);
        } catch (TimeoutException ex) {
            abandon(myGeneration);
            return HoldResult.TIMEOUT;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            abandon(myGeneration);
            return HoldResult.TIMEOUT;
        } catch (ExecutionException ex) {
            ServerUtilities.LOGGER.error("External backup hold could not drain queued writes", ex.getCause());
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
            myGeneration = generation;
            state = State.RELEASING;
        }

        HoldResult result = release(myGeneration, true);
        if (result == HoldResult.OK) ServerUtilities.LOGGER.info("External backup hold released");
        return HoldResponse.of(result);
    }

    public HoldResponse status() {
        if (!backups.enable_external_holds) return HoldResponse.of(HoldResult.DISABLED);
        long now = clock.getAsLong();
        synchronized (lock) {
            if (state == State.PREPARING || state == State.RELEASING
                    || state == State.IDLE && (savingSuspended || pendingDrain != null && !pendingDrain.isDone()
                            || !deferredPlayerWrites.isEmpty()
                            || deferredRanks != null)) {
                return HoldResponse.of(HoldResult.BUSY);
            }
            if (state == State.IDLE) return HoldResponse.of(HoldResult.NO_HOLD);
            if (now >= deadlineNanos) return HoldResponse.of(HoldResult.EXPIRED);
            return HoldResponse.granted(token, remainingSeconds(now), false);
        }
    }

    /** Server-thread lease expiry and retry for an unconfirmed release. */
    public void tick() {
        serverThread = Thread.currentThread();
        long releaseGeneration = -1L;
        synchronized (lock) {
            if (state == State.IDLE) {
                if (savingSuspended) releaseGeneration = generation;
                else if ((!deferredPlayerWrites.isEmpty() || deferredRanks != null)
                        && clock.getAsLong() >= nextDeferredRetryNanos)
                    flushDeferredLocked();
            } else if (state == State.RELEASING) {
                releaseGeneration = generation;
            } else if (state == State.PREPARING && clock.getAsLong() >= deadlineNanos) {
                ServerUtilities.LOGGER.warn("External backup hold preparation timed out; resuming world saving");
                state = State.RELEASING;
                releaseGeneration = generation;
            } else if (state == State.HELD) {
                long now = clock.getAsLong();
                if (now >= deadlineNanos) {
                    ServerUtilities.LOGGER.warn(
                            "External backup hold expired without being released; resuming world saving. "
                                    + "Any backup taken from it is incomplete and must be discarded.");
                    state = State.RELEASING;
                    releaseGeneration = generation;
                } else if (now >= nextWarnNanos) {
                    ServerUtilities.LOGGER.warn(
                            "External backup hold has held world saving for {} seconds",
                            TimeUnit.NANOSECONDS.toSeconds(now - startedNanos));
                    nextWarnNanos = now + TimeUnit.SECONDS.toNanos(backups.external_hold_warn_seconds);
                }
            }
        }
        if (releaseGeneration >= 0) finishRelease(releaseGeneration, false);
    }

    /**
     * Releases through the server thread and waits for it. For callers that are not the server thread, such as an
     * administrator running the stop command over RCON.
     */
    public HoldResponse forceRelease(String reason) {
        final long myGeneration;
        synchronized (lock) {
            if (state == State.IDLE && !savingSuspended && deferredPlayerWrites.isEmpty() && deferredRanks == null) {
                return HoldResponse.of(HoldResult.NO_HOLD);
            }
            myGeneration = generation;
            state = State.RELEASING;
        }
        HoldResult result = release(myGeneration, false);
        ServerUtilities.LOGGER
                .warn("External backup hold {}: {}", result == HoldResult.OK ? "released" : "release pending", reason);
        return HoldResponse.of(result);
    }

    /**
     * Releases inline. Server thread only, for shutdown: the tick loop has stopped by then, so a dispatched release
     * would never run.
     */
    public void forceReleaseNow(String reason) {
        long myGeneration;
        synchronized (lock) {
            if (state == State.IDLE && !savingSuspended && deferredPlayerWrites.isEmpty() && deferredRanks == null)
                return;
            state = State.RELEASING;
            myGeneration = generation;
        }
        HoldResult result = finishRelease(myGeneration, false);
        ServerUtilities.LOGGER
                .warn("External backup hold {}: {}", result == HoldResult.OK ? "released" : "release failed", reason);
    }

    private HoldResult validate(String suppliedToken, long now) {
        if (!backups.enable_external_holds) return HoldResult.DISABLED;
        // The caller supplied a token, so it believes it holds a lease. If nothing is held, the watchdog, an admin or
        // shutdown already took it away; that is EXPIRED, not NO_HOLD, which is reserved for status on an idle server.
        if (state != State.HELD) return HoldResult.EXPIRED;
        if (now >= deadlineNanos) return HoldResult.EXPIRED;
        if (token == null || !token.equals(suppliedToken)) return HoldResult.BAD_TOKEN;
        return null;
    }

    private void abandon(long myGeneration) {
        long releaseGeneration;
        boolean resume;
        synchronized (lock) {
            if (generation != myGeneration) return;
            resume = savingSuspended;
            clearLocked();
            releaseGeneration = generation;
            if (!resume) flushDeferredLocked();
        }
        if (resume) {
            Runnable release = () -> finishRelease(releaseGeneration, false);
            if (Thread.currentThread() == serverThread) release.run();
            else dispatcher.accept(release);
        }
    }

    private HoldResult release(long myGeneration, boolean checkDeadline) {
        if (Thread.currentThread() == serverThread) return finishRelease(myGeneration, checkDeadline);
        CountDownLatch released = new CountDownLatch(1);
        AtomicReference<HoldResult> result = new AtomicReference<>(HoldResult.TIMEOUT);
        dispatcher.accept(() -> {
            try {
                result.set(finishRelease(myGeneration, checkDeadline));
            } finally {
                released.countDown();
            }
        });
        try {
            return released.await(backups.external_hold_prepare_timeout_seconds, TimeUnit.SECONDS) ? result.get()
                    : HoldResult.TIMEOUT;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return HoldResult.TIMEOUT;
        }
    }

    /** Server thread only. The generation check and resume are atomic against a new begin. */
    private HoldResult finishRelease(long myGeneration, boolean checkDeadline) {
        synchronized (lock) {
            if (generation != myGeneration) return HoldResult.EXPIRED;
            try {
                if (savingSuspended) backend.resume();
            } catch (Throwable ex) {
                ServerUtilities.LOGGER.error("External backup hold could not resume world saving", ex);
                return HoldResult.SAVE_FAILED;
            }
            savingSuspended = false;
            boolean expired = checkDeadline && clock.getAsLong() >= deadlineNanos;
            clearLocked();
            flushDeferredLocked();
            if (!deferredPlayerWrites.isEmpty() || deferredRanks != null) return HoldResult.SAVE_FAILED;
            return expired ? HoldResult.EXPIRED : HoldResult.OK;
        }
    }

    private void flushDeferredLocked() {
        Iterator<Map.Entry<UUID, Runnable>> writes = deferredPlayerWrites.entrySet().iterator();
        while (writes.hasNext()) {
            Map.Entry<UUID, Runnable> entry = writes.next();
            try {
                entry.getValue().run();
                writes.remove();
            } catch (Throwable ex) {
                ServerUtilities.LOGGER.error("Could not write deferred player data after backup hold", ex);
            }
        }
        if (deferredRanks != null) {
            try {
                deferredRanks.save();
                deferredRanks = null;
            } catch (Throwable ex) {
                ServerUtilities.LOGGER.error("Could not write deferred ranks after backup hold", ex);
            }
        }
        nextDeferredRetryNanos = deferredPlayerWrites.isEmpty() && deferredRanks == null ? 0L
                : clock.getAsLong() + TimeUnit.SECONDS.toNanos(30);
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
            deferredPlayerWrites.clear();
            deferredRanks = null;
            pendingDrain = null;
            nextDeferredRetryNanos = 0L;
        }
        serverThread = null;
        clock = System::nanoTime;
        backend = LIVE_BACKEND;
        dispatcher = ServerUtilitiesServerEventHandler::scheduleServerTask;
    }
}
