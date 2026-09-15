package serverutils.task.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayDeque;
import java.util.Deque;
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

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.lib.util.FileUtils;

/**
 * Time is driven by hand and the server thread is simulated, so nothing here sleeps or needs a world. The behaviour
 * under test is which answer a client gets, and - the property that actually matters - whether world saving is left on
 * afterwards down every failure path.
 */
public class ExternalBackupHoldTest {

    private final ExternalBackupHold hold = ExternalBackupHold.INSTANCE;
    private final AtomicLong now = new AtomicLong();
    private final RecordingBackend backend = new RecordingBackend();

    /** Stands in for the world: counts suspends and resumes, and can be told to fail in each of the ways it can. */
    private static class RecordingBackend implements ExternalBackupHold.Backend {

        final AtomicInteger suspends = new AtomicInteger();
        final AtomicInteger resumes = new AtomicInteger();
        volatile boolean failSave;
        volatile boolean throwErrorOnSave;
        volatile boolean failDrain;
        volatile boolean suspended;
        volatile Boolean busyOverride;

        @Override
        public void saveAndSuspend() throws Exception {
            if (throwErrorOnSave) throw new StackOverflowError("save blew the stack");
            if (failSave) throw new IllegalStateException("save failed");
            suspends.incrementAndGet();
            suspended = true;
        }

        @Override
        public void resume() {
            resumes.incrementAndGet();
            suspended = false;
        }

        @Override
        public void drain() {
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
        // The refused request must not have touched the world.
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

        // The second request arrives while the first is still preparing.
        HoldResult second = hold.begin(0).result;
        // A client polling mid-preparation must not be told a hold exists.
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
        // The client learns its capture is void rather than being told there was never a hold.
        assertEquals(HoldResult.EXPIRED, hold.end(granted.token).result);
    }

    @Test
    public void renewCannotResurrectAnExpiredLease() {
        HoldResponse granted = hold.begin(60);
        now.set(seconds(61));

        // The deadline passed but the watchdog has not run yet; renew must still refuse it.
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

        // The server thread finally drains the queue long after the request gave up.
        hold.dispatcher = Runnable::run;
        while (!queued.isEmpty()) queued.poll().run();

        assertEquals("the abandoned task must not suspend saving", 0, backend.suspends.get());
        assertFalse(backend.suspended);
        assertFalse(hold.isHeld());
    }

    @Test
    public void aReleaseThatNeverConfirmsLeavesTheWatchdogArmed() {
        // The worst failure this feature can have: saving suspended, no hold recorded, and nothing left watching it.
        hold.begin(60);
        assertTrue(backend.suspended);

        useDroppingDispatcher();
        assertEquals(HoldResult.TIMEOUT, hold.end(hold.status().token).result);

        assertTrue("the hold must not be dropped while saving is still suspended", hold.isHeld());
        assertTrue(backend.suspended);

        // The server thread comes back; the watchdog has to put saving back on without anyone asking again.
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

        // The hold owns the saving states, so the task must return before its cleanup pass touches them.
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
            // An internal backup that has not run its cleanup yet still owns the saving states.
            assertTrue(BackupTask.isWorldSavingSuspended());

            // The check runs on the server thread, inside the dispatched task, where it cannot race the backup.
            assertEquals(HoldResult.BUSY, hold.begin(0).result);
            assertFalse(hold.isHeld());
            assertEquals(0, backend.suspends.get());
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
        // Any other sender, including an opped player, must be rejected before the command runs.
        assertFalse(ExternalBackupHold.isConsoleOrRcon(mock(ICommandSender.class)));
    }

    /**
     * The documented backup coverage names these paths. Moving them is fine, but docs/external-backup-plan.md and the
     * admin documentation have to move with them, so fail loudly here rather than letting the docs rot.
     */
    @Test
    public void documentedGlobalFilePathsAreUnchanged() {
        assertEquals("serverutilities/server/", ServerUtilities.SERVER_FOLDER);
    }
}
