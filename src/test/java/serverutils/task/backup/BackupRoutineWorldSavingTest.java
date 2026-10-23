package serverutils.task.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BackupRoutineWorldSavingTest {

    private WorldServer world;
    private final Object owner = new Object();

    @Before
    public void beforeEach() throws ReflectiveOperationException {
        world = mockWorld(false);
    }

    @After
    public void afterEach() {
        BackupRoutine.resumeWorldSaving();
    }

    private void suspend(Object owner) throws MinecraftException {
        BackupRoutine.suspendWorldSaving(owner, new WorldServer[] { world, null });
    }

    /** Dimensions that load mid-backup are logged by id, so they need a provider. */
    private static WorldServer mockWorld(boolean levelSaving) throws ReflectiveOperationException {
        WorldServer mock = mock(WorldServer.class);
        Field provider = World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(mock, mock(WorldProvider.class));
        mock.levelSaving = levelSaving;
        return mock;
    }

    private static void assertBlocked(Future<?>... futures) throws InterruptedException {
        Thread.sleep(200);
        for (Future<?> future : futures) {
            assertFalse("Must wait for the in-flight suspension to finish", future.isDone());
        }
    }

    @Test(timeout = 30_000)
    public void suspendWorldSaving_HoldsLockAcrossFlush() throws Exception {
        world.levelSaving = false;
        CountDownLatch inFlush = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            inFlush.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("never released");
            return null;
        }).when(world).saveAllChunks(true, null);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            Future<?> suspending = pool.submit(() -> {
                suspend(owner);
                return null;
            });
            assertTrue(inFlush.await(5, TimeUnit.SECONDS));

            // Anything that got in here would see (or undo) a half-finished suspension.
            Future<?> resuming = pool.submit(() -> { BackupRoutine.resumeWorldSaving(); });
            Future<?> querying = pool.submit(BackupRoutine::isWorldSavingSuspended);
            Future<?> loading = pool.submit(() -> { BackupRoutine.suspendLoadedWorld(world); });
            assertBlocked(resuming, querying, loading);

            release.countDown();
            suspending.get(5, TimeUnit.SECONDS);
            resuming.get(5, TimeUnit.SECONDS);
            querying.get(5, TimeUnit.SECONDS);
            loading.get(5, TimeUnit.SECONDS);

            // Without the lock the resume lands mid-flush and the suspension then re-disables saving for good.
            assertFalse(BackupRoutine.isWorldSavingSuspended());
            assertFalse("Original state must be restored", world.levelSaving);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test(timeout = 60_000)
    public void worldSaving_UnderConcurrentUse_NeverCorruptsState() throws Exception {
        int threads = 8;
        int operations = 5_000;
        WorldServer[] worlds = new WorldServer[4];
        List<WorldServer> all = new ArrayList<>();
        for (int i = 0; i < worlds.length; i++) {
            worlds[i] = mockWorld(i % 2 == 0);
            all.add(worlds[i]);
        }
        // Dimensions that load while a backup is running.
        WorldServer[] late = { mockWorld(true), mockWorld(false) };
        all.addAll(Arrays.asList(late));
        List<Boolean> original = new ArrayList<>();
        for (WorldServer each : all) original.add(each.levelSaving);

        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                Object threadOwner = new Object();
                pool.execute(() -> {
                    try {
                        start.await();
                        ThreadLocalRandom random = ThreadLocalRandom.current();
                        for (int i = 0; i < operations; i++) {
                            switch (random.nextInt(5)) {
                                case 0:
                                    try {
                                        BackupRoutine.suspendWorldSaving(threadOwner, worlds);
                                    } catch (IllegalStateException alreadySuspended) {
                                        // Expected whenever another thread holds the suspension.
                                    }
                                    break;
                                case 1:
                                    BackupRoutine.resumeWorldSaving(threadOwner);
                                    break;
                                case 2:
                                    BackupRoutine.resumeWorldSaving();
                                    break;
                                case 3:
                                    BackupRoutine.suspendLoadedWorld(late[random.nextInt(late.length)]);
                                    break;
                                default:
                                    BackupRoutine.isWorldSavingSuspended();
                            }
                        }
                    } catch (Throwable e) {
                        failures.add(e);
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue("Deadlocked or stalled", done.await(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        if (!failures.isEmpty()) throw new AssertionError("Concurrent use failed", failures.peek());

        BackupRoutine.resumeWorldSaving();
        assertFalse(BackupRoutine.isWorldSavingSuspended());
        for (int i = 0; i < all.size(); i++) {
            assertEquals("World " + i + " must end in its original state", original.get(i), all.get(i).levelSaving);
        }
    }

    @Test
    public void suspendWorldSaving_WhenSavingAlreadyOff_StillSavesAndKeepsItOff() throws MinecraftException {
        world.levelSaving = true;
        doAnswer(invocation -> {
            assertFalse("Backup must force a current chunk save", world.levelSaving);
            return null;
        }).when(world).saveAllChunks(true, null);

        suspend(owner);
        verify(world).saveAllChunks(true, null);
        assertTrue(world.levelSaving);
        assertTrue(BackupRoutine.isWorldSavingSuspended());

        BackupRoutine.resumeWorldSaving(owner);
        assertTrue("Saving was off before the backup, so it stays off", world.levelSaving);
        assertFalse(BackupRoutine.isWorldSavingSuspended());
    }

    @Test
    public void suspendWorldSaving_WhenSavingOn_RestoresItAfterwards() throws MinecraftException {
        world.levelSaving = false;

        suspend(owner);
        assertTrue(world.levelSaving);

        BackupRoutine.resumeWorldSaving(owner);
        assertFalse(world.levelSaving);
    }

    @Test
    public void suspendWorldSaving_WhenSaveFails_RestoresBeforeThrowing() throws MinecraftException {
        world.levelSaving = false;
        MinecraftException failure = new MinecraftException("save failed");
        doThrow(failure).when(world).saveAllChunks(true, null);

        assertSame(failure, assertThrows(MinecraftException.class, () -> suspend(owner)));
        assertFalse(world.levelSaving);
        assertFalse(BackupRoutine.isWorldSavingSuspended());
    }

    @Test
    public void suspendWorldSaving_WhenAlreadySuspended_RejectsAndLeavesFirstAlone() throws MinecraftException {
        world.levelSaving = false;
        Object other = new Object();

        suspend(owner);
        assertThrows(IllegalStateException.class, () -> suspend(other));

        assertTrue("The rejected attempt must not resume saving", world.levelSaving);
        assertTrue(BackupRoutine.isWorldSavingSuspended());

        // The rejected routine's cleanup still runs, and must not undo the first backup's suspension.
        BackupRoutine.resumeWorldSaving(other);
        assertTrue(world.levelSaving);
        assertTrue(BackupRoutine.isWorldSavingSuspended());

        BackupRoutine.resumeWorldSaving(owner);
        assertFalse("The original state is restored by the owner", world.levelSaving);
        assertFalse(BackupRoutine.isWorldSavingSuspended());
    }

    @Test
    public void suspendWorldSaving_AfterResume_Succeeds() throws MinecraftException {
        suspend(owner);
        BackupRoutine.resumeWorldSaving(owner);

        suspend(new Object());

        assertTrue(BackupRoutine.isWorldSavingSuspended());
    }

    @Test
    public void resumeWorldSaving_WhenNothingSuspended_DoesNothing() {
        world.levelSaving = true;

        BackupRoutine.resumeWorldSaving(owner);
        BackupRoutine.resumeWorldSaving();

        assertTrue(world.levelSaving);
    }

    @Test
    public void resumeWorldSaving_WithoutOwner_ResumesWhoeverSuspended() throws MinecraftException {
        world.levelSaving = false;

        suspend(owner);
        BackupRoutine.resumeWorldSaving();

        assertFalse(world.levelSaving);
        assertFalse(BackupRoutine.isWorldSavingSuspended());
    }
}
