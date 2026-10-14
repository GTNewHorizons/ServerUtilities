package serverutils.task.backup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import serverutils.ServerUtilitiesConfig;

public class BackupCoordinatorTest extends BackupFixtures.Tests {
    @Before
    public void beforeEach() {
        testSetup();
    }

    @After
    public void afterEach() {
        testReset();
    }

    @Test
    public void execute_DuringExecutingRetention_ShouldRunAfterWorkerFinishes() throws Exception {
        RetentionRoutine routine = mock(RetentionRoutine.class);
        when(routine.preview()).thenAnswer(i -> {
            Thread.sleep(1000);
            return null;
        });

        RetentionRoutine secondRoutine = mock(RetentionRoutine.class);

        CompletableFuture<?> first = BackupCoordinator.execute(routine, RetentionRoutine::preview);
        CompletableFuture<?> second = BackupCoordinator.execute(secondRoutine, RetentionRoutine::prune);

        first.get(5, TimeUnit.SECONDS);
        second.get(5, TimeUnit.SECONDS);
        verify(secondRoutine).prune();
    }

    @Test
    public void startBackup_WhenBackupFails_DoesNotRunRetention()
            throws ExecutionException, InterruptedException, TimeoutException {
        BackupResult.Save saveResult = new BackupResult.Save(world, false);
        saveResult.error = new RuntimeException("Test");

        BackupRoutine routine = mock(BackupRoutine.class);
        when(routine.save(any())).thenReturn(CompletableFuture.completedFuture(saveResult));

        CompletableFuture<BackupResult> finished = new CompletableFuture<>();
        assertTrue(BackupCoordinator.startBackup(routine, finished::complete));

        BackupResult result = finished.get(5, TimeUnit.SECONDS);
        assertSame(saveResult, result.save);
        assertNull(result.retention);
        assertFalse(BackupCoordinator.isRunning());
    }

    @Test
    public void startBackup_WhenBackupSucceeds_RetentionPruneIsExecuted()
            throws ExecutionException, InterruptedException, TimeoutException {
        BackupResult.Save saveResult = new BackupResult.Save(world, false);
        saveResult.destination = backups;

        BackupRoutine routine = mock(BackupRoutine.class);
        when(routine.save(any())).thenReturn(CompletableFuture.completedFuture(saveResult));

        CompletableFuture<BackupResult> finished = new CompletableFuture<>();
        assertTrue(BackupCoordinator.startBackup(routine, finished::complete));

        BackupResult result = finished.get(5, TimeUnit.SECONDS);
        BackupResult.Retention retention = result.retention;

        assertSame(saveResult, result.save);
        assertNotNull(retention);
        assertFalse(BackupCoordinator.isRunning());
        assertTrue(retention.isSuccess());
    }

    @Test
    public void startBackup_WhenRetentionConfigFailsDuringBackup_ErrorIsPassedGracefully() throws ExecutionException, InterruptedException, TimeoutException {
        ServerUtilitiesConfig.backups.retention_policy = null;

        BackupResult.Save saveResult = new BackupResult.Save(world, false);
        saveResult.destination = backups;

        BackupRoutine routine = mock(BackupRoutine.class);
        when(routine.save(any())).thenReturn(CompletableFuture.completedFuture(saveResult));

        CompletableFuture<BackupResult> finished = new CompletableFuture<>();
        assertTrue(BackupCoordinator.startBackup(routine, finished::complete));

        BackupResult result = finished.get(5, TimeUnit.SECONDS);
        BackupResult.Retention retention = result.retention;

        assertNotNull(retention);
        assertFalse(BackupCoordinator.isRunning());
        assertFalse(retention.isSuccess());
        assertNotNull(retention.error);
    }

    @Test
    public void stop_WhenWorkerStuck_WaitUntilTimeoutToExit() throws InterruptedException {
        BackupCoordinator.STOP_TIMEOUT_SECONDS = 5L;

        BackupRoutine routine = mock(BackupRoutine.class);
        when(routine.save(any())).thenAnswer(i -> {
            ExecutorService executorService = i.getArgument(0);
            Future<?> future = executorService.submit(() -> {
                Thread.sleep(3000);
                return new BackupResult.Save(world, false);
            });
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        BackupCoordinator.startBackup(routine);

        Instant startTime = Instant.now();
        BackupCoordinator.stop(); // sorry to people waiting 30 seconds for test to run

        Duration duration = Duration.between(startTime, Instant.now());
        assertTrue(duration.getSeconds() >= 2);

        // Wait for worker to exit
        Thread.sleep(3000);
    }

    // test 30 second timeout while worker exists
}
