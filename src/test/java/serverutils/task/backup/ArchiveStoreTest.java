package serverutils.task.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ArchiveStoreTest extends BackupFixtures.Tests {

    @Before
    public void beforeEach() {
        testSetup(SetupFlags.FOLDERS);
    }

    @After
    public void afterEach() {
        testReset();
    }

    @Test
    public void list_SkipsDirectoriesAndNonArchives() throws IOException {
        File archive = new File(backups, "archive.zip");
        BackupFixtures.writeArchive(archive, UUID.randomUUID().toString(), 1_000);
        Files.createDirectory(new File(backups, "directory.zip").toPath());
        Files.write(new File(backups, "noise.txt").toPath(), new byte[1_000]);
        // an archive still being written is not one retention may delete
        Files.write(new File(backups, ".su-save-123.tmp").toPath(), new byte[1_000]);

        List<ArchiveEntry> entries = new ArchiveStore(backups).list();

        assertEquals(1, entries.size());
        assertEquals(archive, entries.get(0).file);
        assertNull(entries.get(0).problem());
    }

    @Test
    public void list_WhenArchiveUnreadable_KeepsItWithProblem() throws IOException {
        File unreadable = new File(backups, "unreadable.zip");
        Files.write(unreadable.toPath(), new byte[] { 1, 2, 3 });

        List<ArchiveEntry> entries = new ArchiveStore(backups).list();

        assertEquals(1, entries.size());
        assertEquals(unreadable, entries.get(0).file);
        assertNotNull(entries.get(0).problem());
    }
}
