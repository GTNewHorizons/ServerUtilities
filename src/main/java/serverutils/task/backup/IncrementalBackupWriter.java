package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;

import javax.annotation.Nullable;

import net.minecraftforge.common.DimensionManager;

import serverutils.ServerUtilities;
import serverutils.lib.util.compression.ICompress;
import serverutils.task.backup.repository.Repository;
import serverutils.task.backup.repository.RepositoryWriter;

/**
 * Writes a backup into the incremental repository instead of a ZIP. The file ThreadBackup hands over becomes the small
 * archive file; the data goes into the repository's packs.
 */
public class IncrementalBackupWriter implements ICompress {

    /** A repository tool command may briefly hold the lock; a long restore makes this backup fail instead of wait. */
    private static final long LOCK_TIMEOUT_MILLIS = 30_000;

    private final File repositoryFolder;
    private final Properties header = new Properties();
    private Repository repository;
    private RepositoryWriter writer;
    private File output;

    public IncrementalBackupWriter() {
        this(BackupTask.REPOSITORY_FOLDER);
    }

    IncrementalBackupWriter(File repositoryFolder) {
        this.repositoryFolder = repositoryFolder;
    }

    @Override
    public void createOutputStream(File file) throws IOException {
        output = file;
        repository = Repository.openOrCreate(repositoryFolder, LOCK_TIMEOUT_MILLIS);
        for (String warning : repository.warnings()) ServerUtilities.LOGGER.warn("Backup repository: {}", warning);
        writer = repository.newWriter(backups.compression_level);
        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        if (worldDir != null) header.setProperty("worldName", worldDir.getName());
    }

    /** The same details a ZIP backup keeps in its metadata entry. */
    void describe(@Nullable String worldId, long createdAt, boolean customName) {
        if (worldId != null) header.setProperty("worldId", worldId);
        header.setProperty("createdAt", Long.toString(createdAt));
        header.setProperty("customName", Boolean.toString(customName));
    }

    @Override
    public void addFileToArchive(File file, String name) throws IOException {
        writer.addFile(name, file);
    }

    @Override
    public void addStreamToArchive(InputStream input, ZipEntry entry) throws IOException {
        writer.addStream(entry.getName(), entry.getTime(), input, entry.getSize(), (int) entry.getCrc());
    }

    @Override
    public void finish() throws IOException {
        if (!header.containsKey("createdAt")) describe(null, System.currentTimeMillis(), false);
        writer.commit(header, output);
        ServerUtilities.LOGGER.info(
                "Incremental backup stored {} new chunks ({} bytes), reused {} chunks, skipped reading {} unchanged files",
                writer.newChunks,
                writer.storedBytes,
                writer.reusedChunks,
                writer.unchangedFiles);
    }

    /** Bytes this backup added to the repository. */
    long storedBytes() {
        return writer == null ? 0 : writer.storedBytes;
    }

    @Override
    public void extractArchive(File archive, boolean includeGlobal, boolean isOldBackup, File preserved, File recovery)
            throws IOException {
        throw new IOException("Incremental backups are restored with the repository tool, see README.txt");
    }

    @Override
    public boolean isOldBackup(File archive) throws IOException {
        throw new IOException("Not a ZIP backup");
    }

    @Override
    public @Nullable String getWorldName(File file) {
        return null;
    }

    @Override
    public void close() throws IOException {
        try {
            if (writer != null) writer.close();
        } finally {
            if (repository != null) repository.close();
        }
    }
}
