package serverutils.task.backup;

public enum BackupFormat {
    /** A complete, independent ZIP archive per backup. */
    ZIP,
    /** Deduplicated archives in a repository, see {@link serverutils.task.backup.repository.Repository}. */
    INCREMENTAL
}
