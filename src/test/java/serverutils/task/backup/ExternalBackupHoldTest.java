package serverutils.task.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.util.FileUtils;
import serverutils.ranks.Ranks;

/** Controlled time and world operations exercise lease responses and saving restoration. */
public class ExternalBackupHoldTest {

    private final ExternalBackupHold hold = ExternalBackupHold.INSTANCE;
    private final AtomicLong now = new AtomicLong();
    private final RecordingBackend backend = new RecordingBackend();

    private static class RecordingBackend implements ExternalBackupHold.Backend {

        final AtomicInteger suspends = new AtomicInteger();
        final AtomicInteger resumes = new AtomicInteger();
        volatile boolean failSave;
        volatile boolean throwErrorOnSave;
        volatile boolean failDrain;
        volatile boolean failResume;
        volatile boolean suspended;
        volatile Boolean busyOverride;
        volatile CountDownLatch insideDrain;
        volatile CountDownLatch drainGate;

        @Override
        public void saveAndSuspend(Runnable beforeWorldSave) throws Exception {
            if (failSave) throw new IllegalStateException("save failed");
            beforeWorldSave.run();
            if (throwErrorOnSave) throw new StackOverflowError("save blew the stack");
            suspends.incrementAndGet();
            suspended = true;
        }

        @Override
        public void resume() {
            resumes.incrementAndGet();
            if (failResume) throw new IllegalStateException("resume failed");
            suspended = false;
        }

        @Override
        public void drain() throws Exception {
            if (insideDrain != null) insideDrain.countDown();
            if (drainGate != null) drainGate.await();
            if (failDrain) throw new IllegalStateException("drain failed");
        }

        @Override
        public boolean isBusy() {
            if (busyOverride != null) return busyOverride;
            return BackupTask.isBackupRunning() || BackupTask.isWorldSavingSuspended();
        }
    }

    /** BackupTask resolves its backup folder in a static initializer, which needs these before it is first touched. */
    @BeforeClass
    public static void configureBackups() {
        ServerUtilitiesConfig.backups.backup_folder_path = "build/test-backups";
        ServerUtilitiesConfig.backups.backups_to_keep = 12;
        ServerUtilitiesConfig.backups.max_folder_size = 0;
        ServerUtilitiesConfig.backups.delete_custom_name_backups = true;
        ServerUtilitiesConfig.backups.enable_backups = true;
        ServerUtilitiesConfig.backups.need_online_players = false;
        ServerUtilitiesConfig.backups.silent_backup = true;
    }

    @AfterClass
    public static void removeTestBackups() {
        FileUtils.delete(BackupTask.BACKUP_FOLDER);
    }

    @Before
    public void setUp() {
        ServerUtilitiesConfig.backups.enable_external_holds = true;
        ServerUtilitiesConfig.backups.external_hold_default_seconds = 600;
        ServerUtilitiesConfig.backups.external_hold_max_seconds = 1800;
        ServerUtilitiesConfig.backups.external_hold_warn_seconds = 300;
        ServerUtilitiesConfig.backups.external_hold_prepare_timeout_seconds = 5;

        hold.resetForTests();
        now.set(0L);
        hold.clock = now::get;
        hold.backend = backend;
        hold.dispatcher = Runnable::run;
    }

    @After
    public void tearDown() {
        hold.resetForTests();
        ServerUtilitiesConfig.backups.enable_external_holds = false;
    }

    private static long seconds(long value) {
        return TimeUnit.SECONDS.toNanos(value);
    }

    /** A dispatcher that silently drops the task, standing in for a wedged or stopped server thread. */
    private void useDroppingDispatcher() {
        ServerUtilitiesConfig.backups.external_hold_prepare_timeout_seconds = 1;
        hold.dispatcher = task -> {};
    }

    @Test
    public void singletonKeepsItsLiveBackend() {
        hold.resetForTests();
        assertNotNull(hold.backend);
    }

    @Test
    public void beginGrantsAHoldAndSuspendsSaving() {
        HoldResponse response = hold.begin(0);

        assertEquals(HoldResult.OK, response.result);
        assertNotNull(response.token);
        assertEquals(600L, response.secondsLeft);
        assertTrue(hold.isHeld());
        assertEquals(1, backend.suspends.get());
        assertTrue(backend.suspended);
    }

    @Test
    public void beginIsRefusedWhileAnotherHoldIsActive() {
        assertEquals(HoldResult.OK, hold.begin(0).result);

        assertEquals(HoldResult.BUSY, hold.begin(0).result);
        assertEquals(1, backend.suspends.get());
        assertEquals(0, backend.resumes.get());
    }

    @Test
    public void concurrentBeginCallsGrantExactlyOneHold() throws Exception {
        CountDownLatch insideFirst = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        hold.dispatcher = task -> {
            insideFirst.countDown();
            try {
                releaseFirst.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            task.run();
        };

        AtomicReference<HoldResult> first = new AtomicReference<>();
        Thread thread = new Thread(() -> first.set(hold.begin(0).result));
        thread.start();
        assertTrue(insideFirst.await(5, TimeUnit.SECONDS));

        HoldResult second = hold.begin(0).result;
        HoldResult whilePreparing = hold.status().result;

        releaseFirst.countDown();
        thread.join(TimeUnit.SECONDS.toMillis(5));

        assertEquals(HoldResult.OK, first.get());
        assertEquals(HoldResult.BUSY, second);
        assertEquals(HoldResult.BUSY, whilePreparing);
        assertEquals(1, backend.suspends.get());
    }

    @Test
    public void longerThanTheCeilingIsClampedAndSaysSo() {
        HoldResponse response = hold.begin(99999);

        assertEquals(HoldResult.OK, response.result);
        assertEquals(1800L, response.secondsLeft);
        assertTrue(response.clamped);
        assertTrue(response.toString().endsWith(" CLAMPED"));
    }

    @Test
    public void endWithAStaleTokenIsRefusedAndKeepsTheHold() {
        HoldResponse granted = hold.begin(0);

        assertEquals(HoldResult.BAD_TOKEN, hold.end("not-the-token").result);
        assertTrue(hold.isHeld());
        assertEquals(0, backend.resumes.get());

        assertEquals(HoldResult.OK, hold.end(granted.token).result);
        assertFalse(hold.isHeld());
        assertEquals(1, backend.resumes.get());
        assertFalse(backend.suspended);
    }

    @Test
    public void theWatchdogResumesSavingWhenTheLeaseRunsOut() {
        HoldResponse granted = hold.begin(60);

        now.set(seconds(59));
        hold.tick();
        assertTrue("the lease has not run out yet", hold.isHeld());
        assertEquals(0, backend.resumes.get());

        now.set(seconds(60));
        hold.tick();

        assertFalse(hold.isHeld());
        assertEquals(1, backend.resumes.get());
        assertFalse("saving must be back on after expiry", backend.suspended);
        assertEquals(HoldResult.EXPIRED, hold.end(granted.token).result);
    }

    @Test
    public void renewCannotResurrectAnExpiredLease() {
        HoldResponse granted = hold.begin(60);
        now.set(seconds(61));

        assertEquals(HoldResult.EXPIRED, hold.renew(granted.token, 600).result);

        hold.tick();
        assertEquals(HoldResult.EXPIRED, hold.renew(granted.token, 600).result);
        assertFalse(hold.isHeld());
    }

    @Test
    public void renewReportsTheFullLeaseItGranted() {
        HoldResponse granted = hold.begin(60);

        now.set(seconds(30));
        HoldResponse renewed = hold.renew(granted.token, 60);
        assertEquals(HoldResult.OK, renewed.result);
        // Must be the whole lease, not one second less: the reported value is what a client renews against.
        assertEquals(60L, renewed.secondsLeft);

        now.set(seconds(80));
        hold.tick();
        assertTrue("the renewal moved the deadline out", hold.isHeld());
    }

    @Test
    public void aFailedSaveGrantsNoHoldAndLeavesSavingOn() {
        backend.failSave = true;

        assertEquals(HoldResult.SAVE_FAILED, hold.begin(0).result);
        assertFalse(hold.isHeld());
        assertFalse(backend.suspended);
        assertEquals(HoldResult.NO_HOLD, hold.status().result);
    }

    @Test
    public void anErrorDuringSaveGrantsNoHold() {
        // An Error is not an Exception. If it were only caught as one, prepare would see no failure recorded and hand
        // out a hold over a world that was never suspended - a silently corrupt backup.
        backend.throwErrorOnSave = true;

        assertEquals(HoldResult.SAVE_FAILED, hold.begin(0).result);
        assertFalse(hold.isHeld());
        assertFalse(backend.suspended);
        assertEquals(0, backend.suspends.get());
    }

    @Test
    public void aFailedDrainGrantsNoHoldAndResumesSaving() {
        backend.failDrain = true;

        assertEquals(HoldResult.SAVE_FAILED, hold.begin(0).result);
        assertFalse(hold.isHeld());
        assertEquals(1, backend.suspends.get());
        assertEquals("saving must be restored after a failed drain", 1, backend.resumes.get());
        assertFalse(backend.suspended);
    }

    @Test
    public void aDroppedPreparationTimesOutAndGrantsNoHold() {
        useDroppingDispatcher();

        assertEquals(HoldResult.TIMEOUT, hold.begin(0).result);
        assertEquals("nothing reached the world", 0, backend.suspends.get());
    }

    @Test
    public void aPreparationTaskArrivingAfterTimeoutDoesNotSuspend() {
        Deque<Runnable> queued = new ArrayDeque<>();
        ServerUtilitiesConfig.backups.external_hold_prepare_timeout_seconds = 1;
        hold.dispatcher = queued::add;

        assertEquals(HoldResult.TIMEOUT, hold.begin(0).result);

        hold.dispatcher = Runnable::run;
        while (!queued.isEmpty()) queued.poll().run();

        assertEquals("the abandoned task must not suspend saving", 0, backend.suspends.get());
        assertFalse(backend.suspended);
        assertFalse(hold.isHeld());
    }

    @Test
    public void aReleaseThatNeverConfirmsLeavesTheWatchdogArmed() {
        hold.begin(60);
        assertTrue(backend.suspended);

        useDroppingDispatcher();
        assertEquals(HoldResult.TIMEOUT, hold.end(hold.status().token).result);

        assertTrue("the hold must not be dropped while saving is still suspended", hold.isHeld());
        assertTrue(backend.suspended);

        hold.dispatcher = Runnable::run;
        hold.tick();

        assertFalse(hold.isHeld());
        assertFalse("world saving must never be left suspended", backend.suspended);
        assertEquals(1, backend.resumes.get());
    }

    @Test
    public void forceReleaseNowResumesSavingForShutdown() {
        HoldResponse granted = hold.begin(0);

        hold.forceReleaseNow("server is shutting down");

        assertFalse(hold.isHeld());
        assertFalse(backend.suspended);
        assertEquals(HoldResult.EXPIRED, hold.end(granted.token).result);
    }

    @Test
    public void forceReleaseNowOnAnIdleServerDoesNothing() {
        hold.forceReleaseNow("nothing to release");

        assertEquals(0, backend.resumes.get());
    }

    @Test
    public void forceReleaseRoutesThroughTheServerThread() {
        Deque<Runnable> queued = new ArrayDeque<>();
        hold.begin(0);
        hold.dispatcher = queued::add;

        Thread caller = new Thread(() -> hold.forceRelease("released by an admin"));
        caller.start();

        // The resume must be handed to the server thread, not run on the calling thread.
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        while (queued.isEmpty() && System.currentTimeMillis() < deadline) Thread.yield();
        assertFalse("the release must be dispatched, not run inline", queued.isEmpty());
        assertTrue("saving must still be suspended until the server thread runs it", backend.suspended);

        queued.poll().run();
        try {
            caller.join(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        assertFalse(backend.suspended);
        assertFalse(hold.isHeld());
    }

    @Test
    public void statusReportsAnIdleServerAsNoHold() {
        assertEquals(HoldResult.NO_HOLD, hold.status().result);

        hold.begin(60);
        HoldResponse active = hold.status();
        assertEquals(HoldResult.OK, active.result);
        assertEquals(60L, active.secondsLeft);
        assertNotNull(active.token);
    }

    @Test
    public void everyCommandIsRefusedWhileTheFeatureIsDisabled() {
        ServerUtilitiesConfig.backups.enable_external_holds = false;

        assertEquals(HoldResult.DISABLED, hold.begin(0).result);
        assertEquals(HoldResult.DISABLED, hold.status().result);
        assertEquals(HoldResult.DISABLED, hold.renew("token", 0).result);
        assertEquals(HoldResult.DISABLED, hold.end("token").result);
        assertEquals(0, backend.suspends.get());
    }

    @Test
    public void automaticBackupsAreSkippedWhileAHoldIsActive() {
        hold.begin(0);

        new BackupTask().execute(null);
        new BackupTask(true).execute(null);

        assertTrue(hold.isHeld());
        assertEquals("the backup must not have resumed saving", 0, backend.resumes.get());
        assertTrue(backend.suspended);
    }

    @Test
    public void aHoldIsRefusedWhileWorldSavingIsAlreadySuspended() {
        try {
            BackupTask.saveAndDisableWorldSaving(new WorldServer[] { mock(WorldServer.class) });
            assertTrue(BackupTask.isWorldSavingSuspended());

            assertEquals(HoldResult.BUSY, hold.begin(0).result);
            assertFalse(hold.isHeld());
            assertEquals(0, backend.suspends.get());
            assertEquals(0, backend.resumes.get());
            assertTrue(BackupTask.isWorldSavingSuspended());
        } catch (Exception ex) {
            throw new AssertionError(ex);
        } finally {
            BackupTask.restoreWorldSaving();
        }
    }

    @Test
    public void nonOkResponsesCarryNoTokenField() {
        HoldResponse busy = HoldResponse.of(HoldResult.BUSY);

        assertEquals("BUSY", busy.toString());
        assertNull(busy.token);
        assertEquals("INVALID_ARGUMENT", HoldResponse.of(HoldResult.INVALID_ARGUMENT).toString());
    }

    @Test
    public void onlyConsoleAndRconMayHold() {
        assertTrue(ExternalBackupHold.isConsoleOrRcon(mock(MinecraftServer.class)));
        assertFalse(ExternalBackupHold.isConsoleOrRcon(mock(ICommandSender.class)));
    }

    @Test
    public void partialSaveErrorKeepsTheWatchdogArmed() {
        backend.throwErrorOnSave = true;
        AtomicInteger dispatched = new AtomicInteger();
        hold.dispatcher = task -> { if (dispatched.incrementAndGet() == 1) task.run(); };

        assertEquals(HoldResult.SAVE_FAILED, hold.begin(0).result);
        assertTrue(hold.isHeld());
        hold.tick();
        assertFalse(hold.isHeld());
        assertEquals(1, backend.resumes.get());
    }

    @Test
    public void anOldReleaseCannotResumeANewerHold() throws Exception {
        HoldResponse first = hold.begin(60);
        AtomicReference<Runnable> pending = new AtomicReference<>();
        CountDownLatch queued = new CountDownLatch(1);
        hold.dispatcher = task -> {
            pending.set(task);
            queued.countDown();
        };
        AtomicReference<HoldResult> ended = new AtomicReference<>();
        Thread caller = new Thread(() -> ended.set(hold.end(first.token).result));
        caller.start();
        assertTrue(queued.await(5, TimeUnit.SECONDS));

        now.set(seconds(60));
        hold.tick();
        assertEquals(HoldResult.OK, hold.begin(60).result);
        pending.get().run();
        caller.join(TimeUnit.SECONDS.toMillis(5));

        assertEquals(HoldResult.EXPIRED, ended.get());
        assertTrue(hold.isHeld());
        assertTrue(backend.suspended);
        assertEquals(1, backend.resumes.get());
    }

    @Test
    public void renewalAndStatusDoNotConfirmAReleasingHold() throws Exception {
        HoldResponse granted = hold.begin(60);
        AtomicReference<Runnable> pending = new AtomicReference<>();
        CountDownLatch queued = new CountDownLatch(1);
        hold.dispatcher = task -> {
            pending.set(task);
            queued.countDown();
        };
        AtomicReference<HoldResult> ended = new AtomicReference<>();
        Thread caller = new Thread(() -> ended.set(hold.end(granted.token).result));
        caller.start();
        assertTrue(queued.await(5, TimeUnit.SECONDS));

        assertEquals(HoldResult.EXPIRED, hold.renew(granted.token, 60).result);
        assertEquals(HoldResult.BUSY, hold.status().result);
        pending.get().run();
        caller.join(TimeUnit.SECONDS.toMillis(5));
        assertEquals(HoldResult.OK, ended.get());
    }

    @Test
    public void stuckDrainTimesOutAndResumesSaving() throws Exception {
        ServerUtilitiesConfig.backups.external_hold_prepare_timeout_seconds = 1;
        backend.insideDrain = new CountDownLatch(1);
        backend.drainGate = new CountDownLatch(1);
        AtomicReference<HoldResult> response = new AtomicReference<>();
        Thread caller = new Thread(() -> response.set(hold.begin(0).result));
        caller.start();
        assertTrue(backend.insideDrain.await(5, TimeUnit.SECONDS));

        now.set(seconds(1));
        hold.tick();
        caller.join(TimeUnit.SECONDS.toMillis(3));

        try {
            assertEquals(HoldResult.TIMEOUT, response.get());
            assertTrue(hold.isHeld());
            assertFalse(backend.suspended);
            assertEquals(1, backend.resumes.get());
            assertEquals(HoldResult.BUSY, hold.begin(0).result);
        } finally {
            backend.drainGate.countDown();
        }
    }

    @Test
    public void deferredPlayerAndRankWritesRunAfterRelease() {
        UUID playerId = UUID.randomUUID();
        AtomicInteger writes = new AtomicInteger();
        Ranks ranks = mock(Ranks.class);
        assertFalse(hold.deferPlayerWrite(playerId, writes::incrementAndGet));
        HoldResponse granted = hold.begin(0);

        assertTrue(hold.deferPlayerWrite(playerId, writes::incrementAndGet));
        assertTrue(hold.hasDeferredPlayerWrite(playerId));
        assertTrue(hold.deferRankSave(ranks));
        assertEquals(0, writes.get());

        assertEquals(HoldResult.OK, hold.end(granted.token).result);
        assertEquals(1, writes.get());
        assertFalse(hold.hasDeferredPlayerWrite(playerId));
        verify(ranks).save();
    }

    @Test
    public void failedDeferredWriteIsRetriedBeforeAnotherHold() {
        UUID playerId = UUID.randomUUID();
        AtomicInteger attempts = new AtomicInteger();
        HoldResponse granted = hold.begin(0);
        hold.deferPlayerWrite(
                playerId,
                () -> { if (attempts.incrementAndGet() == 1) throw new IllegalStateException("write failed"); });

        assertEquals(HoldResult.OK, hold.end(granted.token).result);
        assertEquals(HoldResult.BUSY, hold.status().result);
        assertEquals(HoldResult.BUSY, hold.begin(0).result);
        assertTrue(hold.hasDeferredPlayerWrite(playerId));

        now.set(seconds(30));
        hold.tick();
        assertEquals(2, attempts.get());
        assertFalse(hold.hasDeferredPlayerWrite(playerId));
        assertEquals(HoldResult.NO_HOLD, hold.status().result);
    }

    @Test
    public void commandsOnTheServerThreadReleaseInline() {
        hold.tick();
        hold.dispatcher = task -> {
            throw new AssertionError("server-thread commands must not wait for a queued task");
        };

        HoldResponse granted = hold.begin(0);
        assertEquals(HoldResult.OK, granted.result);
        assertEquals(HoldResult.OK, hold.end(granted.token).result);
        assertFalse(backend.suspended);
    }

    @Test
    public void watchdogCompletesEndBeforeItsQueuedTaskWithoutExpiringIt() throws Exception {
        HoldResponse first = hold.begin(60);
        AtomicReference<Runnable> pending = new AtomicReference<>();
        CountDownLatch queued = new CountDownLatch(1);
        hold.dispatcher = task -> {
            pending.set(task);
            queued.countDown();
        };
        AtomicReference<HoldResult> ended = new AtomicReference<>();
        Thread caller = new Thread(() -> ended.set(hold.end(first.token).result));
        caller.start();
        assertTrue(queued.await(5, TimeUnit.SECONDS));

        hold.tick();
        caller.join(TimeUnit.SECONDS.toMillis(5));
        assertFalse(caller.isAlive());
        assertEquals(HoldResult.OK, ended.get());
        assertEquals(1, backend.resumes.get());
        assertEquals(HoldResult.OK, hold.begin(60).result);
        pending.get().run();
        assertTrue(backend.suspended);
        assertEquals(1, backend.resumes.get());
    }

    @Test
    public void adminReleaseInvalidatesQueuedEnd() throws Exception {
        HoldResponse granted = hold.begin(60);
        AtomicReference<Runnable> pending = new AtomicReference<>();
        CountDownLatch queued = new CountDownLatch(1);
        hold.dispatcher = task -> {
            pending.set(task);
            queued.countDown();
        };
        AtomicReference<HoldResult> ended = new AtomicReference<>();
        Thread caller = new Thread(() -> ended.set(hold.end(granted.token).result));
        caller.start();
        assertTrue(queued.await(5, TimeUnit.SECONDS));

        hold.forceReleaseNow("admin stop");
        pending.get().run();
        caller.join(TimeUnit.SECONDS.toMillis(5));
        assertFalse(caller.isAlive());
        assertEquals(HoldResult.EXPIRED, ended.get());
        assertFalse(backend.suspended);
    }

    @Test
    public void failedResumeBacksOffButShutdownRetriesImmediately() {
        now.set(seconds(-100));
        HoldResponse granted = hold.begin(60);
        backend.failResume = true;
        assertEquals(HoldResult.SAVE_FAILED, hold.end(granted.token).result);
        for (int i = 0; i < 20; i++) hold.tick();
        assertEquals(1, backend.resumes.get());
        now.set(seconds(-70));
        hold.tick();
        assertEquals(2, backend.resumes.get());
        backend.failResume = false;
        hold.forceReleaseNow("shutdown");
        assertEquals(3, backend.resumes.get());
        assertFalse(backend.suspended);
        assertFalse(hold.isHeld());
    }

    @Test
    public void persistentDeferredFailureRetainsPlayerDataWithoutRejectingCapture() {
        HoldResponse granted = hold.begin(60);
        UUID player = UUID.randomUUID();
        AtomicInteger attempts = new AtomicInteger();
        hold.deferPlayerWrite(player, () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("persistent save failure");
        });
        assertEquals(HoldResult.OK, hold.end(granted.token).result);
        assertFalse(backend.suspended);
        for (int i = 1; i <= 3; i++) {
            now.set(seconds(30L * i));
            hold.tick();
            assertTrue(hold.hasDeferredPlayerWrite(player));
            assertEquals(HoldResult.BUSY, hold.begin(60).result);
        }
        assertEquals(4, attempts.get());
    }
}
