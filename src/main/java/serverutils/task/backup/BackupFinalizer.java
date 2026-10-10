package serverutils.task.backup;

import java.util.function.Consumer;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;



public class BackupFinalizer implements Consumer<Boolean> {
    private Universe universe;

    @Override
    public void accept(Boolean successful) {
        universe.restoreWorldSaving();

        if (worldSaveStates.isEmpty()) return;

        BackupTask.finishPlayerActivity(successful);
        thread = null;
        snapshotThread = null;
        backupSucceeded = false;
        restoreWorldSaving();
        if (successful || prunePending) clearOldBackups();
        FileUtils.delete(BACKUP_TEMP_FOLDER);
    }
}
