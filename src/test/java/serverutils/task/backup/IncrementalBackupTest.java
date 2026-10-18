package serverutils.task.backup;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.util.FileUtils;
import serverutils.task.backup.repository.ArchiveInfo;
import serverutils.task.backup.repository.Repository;
import serverutils.task.backup.repository.RepositoryTool;

public class IncrementalBackupTest {

    private Path world;
    private final String worldId = UUID.randomUUID().toString();

    @BeforeClass
    public static void configureBackups() {
        ServerUtilitiesConfig.backups.backup_folder_path = "build/test-backups";
        ServerUtilitiesConfig.backups.additional_backup_files = new String[0];
        ServerUtilitiesConfig.backups.excluded_backup_files = new String[0];
        ServerUtilitiesConfig.backups.compression_level = 1;
        ServerUtilitiesConfig.backups.only_backup_claimed_chunks = false;
        ServerUtilitiesConfig.backups.silent_backup = true;
    }

    @Before
    public void createWorld() throws IOException {
        Files.createDirectories(new File("build").toPath());
        world = Files.createTempDirectory(new File("build").toPath(), "incremental-world-");
        Files.write(world.resolve("level.dat"), random(800, 1));
        Files.createDirectories(world.resolve("region"));
        Files.write(world.resolve("region/r.0.0.mca"), region(2));
        Files.createDirectories(world.resolve("data"));
        Files.write(world.resolve("data/villages.dat"), random(2000, 3));
        age();
    }

    /** Files saved long enough ago for the files cache to trust their timestamps. */
    private void age() throws IOException {
        FileTime old = FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5));
        try (java.util.stream.Stream<Path> files = Files.walk(world)) {
            for (Path file : files.filter(Files::isRegularFile).collect(Collectors.toList())) {
                Files.setLastModifiedTime(file, old);
            }
        }
    }

    @After
    public void deleteWorldAndRepository() {
        BackupTask.stopBackupThread();
        FileUtils.delete(world.toFile());
        FileUtils.delete(BackupTask.REPOSITORY_FOLDER);
    }

    /** A region file of 32 Minecraft chunks with 3 sectors each, about 400 KB. */
    private static byte[] region(long seed) {
        byte[] region = random(8192 + 32 * 3 * 4096, seed);
        for (int i = 0; i < 1024; i++) {
            int entry = i < 32 ? (2 + i * 3) << 8 | 3 : 0;
            for (int b = 0; b < 4; b++) region[i * 4 + b] = (byte) (entry >>> (24 - 8 * b));
        }
        return region;
    }

    private static byte[] random(int size, long seed) {
        byte[] bytes = new byte[size];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }

    private boolean backup(String name, long created, boolean snapshot, boolean overwrite) throws IOException {
        ThreadBackup.Snapshot captured = snapshot ? ThreadBackup.snapshotFiles(world.toFile()) : null;
        try {
            return ThreadBackup.doBackup(
                    new IncrementalBackupWriter(),
                    world.toFile(),
                    name,
                    Collections.emptySet(),
                    captured,
                    false,
                    worldId,
                    created,
                    overwrite);
        } finally {
            if (captured != null) ThreadBackup.deleteSnapshot();
        }
    }

    private Repository open() throws IOException {
        return Repository.open(BackupTask.REPOSITORY_FOLDER, true, 0);
    }

    @Test
    public void backupsAreDeduplicatedAndRestoreByteForByte() throws Exception {
        long now = System.currentTimeMillis();
        assertTrue(backup("", now - 120_000, true, false));
        long firstSize;
        try (Repository repository = open()) {
            firstSize = repository.diskSize();
        }
        byte[] region = Files.readAllBytes(world.resolve("region/r.0.0.mca"));
        System.arraycopy(random(4096, 9), 0, region, 200_000, 4096);
        Files.write(world.resolve("region/r.0.0.mca"), region);
        age();
        assertTrue(backup("", now - 60_000, false, false));
        try (Repository repository = open()) {
            List<String> names = repository.archiveNames();
            assertEquals(2, names.size());
            ArchiveInfo latest = repository.readInfo(names.get(1));
            assertEquals(worldId, latest.worldId());
            assertEquals(now - 60_000, latest.created());
            assertFalse(latest.isCustomName());
            assertEquals(FileUtils.getRelativePath(world.toFile()) + "/", latest.worldPath());
            long added = repository.diskSize() - firstSize;
            assertTrue("Second backup added " + added + " bytes", added < 100_000);
        }

        Path target = Files.createTempDirectory(new File("build").toPath(), "incremental-restore-");
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            java.io.PrintStream print = new java.io.PrintStream(output, true);
            int status = new RepositoryTool(print, print).run(
                    new String[] { "--repo", BackupTask.REPOSITORY_FOLDER.getPath(), "restore", "latest",
                            target.toString(), "--world-only" });
            assertEquals(output.toString(), 0, status);
            String relative = FileUtils.getRelativePath(world.toFile());
            for (String file : new String[] { "level.dat", "region/r.0.0.mca", "data/villages.dat" }) {
                assertArrayEquals(
                        file,
                        Files.readAllBytes(world.resolve(file)),
                        Files.readAllBytes(target.resolve(relative).resolve(file)));
            }
        } finally {
            FileUtils.delete(target.toFile());
        }
    }

    @Test
    public void customNamesNeedOverwriteToBeReplaced() throws Exception {
        long now = System.currentTimeMillis();
        assertTrue(backup("checkpoint", now - 1000, false, false));
        assertThrows(IOException.class, () -> ThreadBackup.backupDestination("checkpoint", false, now, true));
        assertTrue(backup("checkpoint", now, false, true));
        try (Repository repository = open()) {
            assertEquals(Collections.singletonList("checkpoint"), repository.archiveNames());
            ArchiveInfo info = repository.readInfo("checkpoint");
            assertEquals(now, info.created());
            assertTrue(info.isCustomName());
        }
        // The ZIP folder is not touched by incremental backups.
        assertFalse(new File(BackupTask.BACKUP_FOLDER, "checkpoint.zip").exists());
    }

    @Test
    public void retentionPrunesTheRepositoryAndFreesUnsharedData() throws Exception {
        long now = System.currentTimeMillis();
        for (int i = 3; i >= 1; i--) {
            // Each backup has a different region, so deleting one frees its data.
            Files.write(world.resolve("region/r.0.0.mca"), region(100 + i));
            age();
            assertTrue(backup("", now - TimeUnit.HOURS.toMillis(i), false, false));
        }
        try (Repository repository = open()) {
            BackupRetention.Plan byCount = RepositoryRetention.plan(repository, new String[0], 2, now, true, 0);
            assertEquals(1, byCount.delete.size());
            assertTrue("Freeing one region", byCount.deletedSize > 390_000);

            BackupRetention.Plan bySize = RepositoryRetention.plan(repository, new String[0], 12, now, true, 1);
            assertEquals("Only the latest backup is always kept", 2, bySize.delete.size());
            assertTrue(bySize.remainingRotationSize > 1);

            BackupRetention.Plan byPolicy = RepositoryRetention
                    .plan(repository, new String[] { "90m:all" }, 0, now, true, 0);
            assertEquals(2, byPolicy.delete.size());

            long before = repository.diskSize();
            RepositoryRetention.apply(repository, byCount, 0);
            assertEquals(2, repository.archiveNames().size());
            assertTrue(repository.diskSize() < before - 390_000);
            assertTrue(repository.check(true, message -> {}).ok());
        }
    }

    @Test
    public void listingAndPreviewIncludeIncrementalArchives() throws Exception {
        long now = System.currentTimeMillis();
        assertTrue(backup("", now - 1000, false, false));
        List<BackupRetention.Archive> archives = BackupTask.listBackupsAsync().get(10, TimeUnit.SECONDS);
        List<BackupRetention.Archive> incremental = archives.stream().filter(archive -> archive.incremental)
                .collect(Collectors.toList());
        assertEquals(1, incremental.size());
        assertEquals(now - 1000, incremental.get(0).created);

        String[] policy = ServerUtilitiesConfig.backups.retention_policy;
        try {
            ServerUtilitiesConfig.backups.retention_policy = new String[] { "1h:all" };
            BackupRetention.Plan plan = BackupTask.previewRetention();
            assertTrue(plan.keep.containsKey(incremental.get(0).file));
        } finally {
            ServerUtilitiesConfig.backups.retention_policy = policy;
        }
    }
}
