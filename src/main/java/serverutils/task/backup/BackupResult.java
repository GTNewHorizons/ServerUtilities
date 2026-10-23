package serverutils.task.backup;

import java.io.File;
import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nullable;

import serverutils.lib.util.StringUtils;

public class BackupResult {

    public final Save save;
    /** The prune that ran after this backup, or null if none ran. */
    @Nullable
    public final Retention retention;

    public BackupResult(Save save, @Nullable Retention retention) {
        this.save = save;
        this.retention = retention;
    }

    public boolean isSuccess() {
        return save.isSuccess();
    }

    public static class Save {

        public final File world;
        public final boolean isIncremental;

        public File destination;

        public final Instant startTime = Instant.now();
        public Duration duration = Duration.ZERO;

        public int unmodifiedChunks;
        public int unmodifiedRegions;

        public int chunks;
        public long bytes;

        public int newFiles;
        public int unmodifiedFiles;

        public Throwable error;

        public Save(File world, boolean incremental) {
            this.world = world;
            this.isIncremental = incremental;
        }

        /** A failed save for a backup that ended before it could report its own result. */
        public static Save failed(Throwable error) {
            Save save = new Save(null, false);
            save.error = error;
            return save.finished();
        }

        public Save finished() {
            duration = Duration.between(startTime, Instant.now());
            return this;
        }

        public boolean isSuccess() {
            return error == null;
        }

        public String getDoneTime() {
            return StringUtils.getTimeString(duration.toMillis());
        }
    }

    public static class Retention {

        public final Instant startTime = Instant.now();
        public Duration duration = Duration.ZERO;

        public long bytesRemoved;
        public int filesRemoved;

        public Throwable error;

        public Retention finished() {
            duration = Duration.between(startTime, Instant.now());
            return this;
        }

        public boolean isSuccess() {
            return error == null;
        }
    }
}
