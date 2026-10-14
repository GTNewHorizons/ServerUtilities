package serverutils.task.backup;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class BackupCoordinatorTest extends BackupFixtures.Tests {

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

    public void runBackup_WhenBackupFails_DoesNotRunRetention() {

    }

    // test 30 second timeout while worker exists
}
