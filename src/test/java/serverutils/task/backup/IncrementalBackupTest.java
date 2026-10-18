package serverutils.task.backup;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import serverutils.lib.util.FileUtils;
import serverutils.task.backup.repository.ArchiveInfo;
import serverutils.task.backup.repository.Repository;
import serverutils.task.backup.repository.RepositoryTool;

public class IncrementalBackupTest extends BackupFixtures.Tests {

    private File repositoryFolder;

    @Before
    public void createWorld() throws IOException {
        testSetup();
        when(universe.getUUID()).thenReturn(BackupFixtures.UUID);
        repositoryFolder = new File(backups, "incremental");
        Path world = this.world.toPath();
        Files.write(world.resolve("level.dat"), random(800, 1));
        Files.createDirectories(world.resolve("region"));
        Files.write(world.resolve("region/r.0.0.mca"), region(2));
        Files.createDirectories(world.resolve("data"));
        Files.write(world.resolve("data/villages.dat"), random(2000, 3));
        age();
    }

    @After
    public void deleteWorldAndRepository() {
        testReset();
    }

    /** Files saved long enough ago for the files cache to trust their timestamps. */
    private void age() throws IOException {
        FileTime old = FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5));
        try (Stream<Path> files = Files.walk(world.toPath())) {
            for (Path file : files.filter(Files::isRegularFile).collect(Collectors.toList())) {
                Files.setLastModifiedTime(file, old);
            }
        }
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

    private BackupRoutine.Options options(String name, long created, boolean overwrite) {
        BackupRoutine.Options options = BackupRoutine.Options.fromConfig();
        options.backupsDirectory = backups;
        options.world = world;
        options.incremental = true;
        options.customName = name.isEmpty() ? null : name;
        options.overwrite = overwrite;
        options.createdAt = Instant.ofEpochMilli(created);
        return options;
    }

    private BackupResult.Save backup(String name, long created, boolean overwrite) {
        BackupResult.Save result = new BackupRoutine(universe, options(name, created, overwrite)).save();
        assertNull(String.valueOf(result.error), result.error);
        return result;
    }

    private Repository open() throws IOException {
        return Repository.open(repositoryFolder, true, 0);
    }

    private RetentionRoutine retention(boolean deleteCustom, long maxSize, int keepCount, String... policy) {
        RetentionRoutine.Options options = new RetentionRoutine.Options(
                Arrays.asList(policy),
                deleteCustom,
                0,
                keepCount);
        options.maxSize = maxSize;
        return new RetentionRoutine(new RepositoryStore(repositoryFolder), options);
    }

    @Test
    public void backupsAreDeduplicatedAndRestoreByteForByte() throws Exception {
        long now = System.currentTimeMillis();
        BackupResult.Save first = backup("", now - 120_000, false);
        assertTrue(first.destination.getName().endsWith(Repository.ARCHIVE_EXTENSION));
        long firstSize;
        try (Repository repository = open()) {
            firstSize = repository.diskSize();
        }
        Path world = this.world.toPath();
        byte[] region = Files.readAllBytes(world.resolve("region/r.0.0.mca"));
        System.arraycopy(random(4096, 9), 0, region, 200_000, 4096);
        Files.write(world.resolve("region/r.0.0.mca"), region);
        age();
        BackupResult.Save second = backup("", now - 60_000, false);
        try (Repository repository = open()) {
            List<String> names = repository.archiveNames();
            assertEquals(2, names.size());
            ArchiveInfo latest = repository.readInfo(names.get(1));
            assertEquals(BackupFixtures.UUID.toString(), latest.worldId());
            assertEquals(now - 60_000, latest.created());
            assertFalse(latest.isCustomName());
            assertEquals(FileUtils.getRelativePath(this.world) + "/", latest.worldPath());
            long added = repository.diskSize() - firstSize;
            assertTrue("Second backup added " + added + " bytes", added < 100_000);
            assertTrue("Reported as the data it added", second.bytes < 100_000);
        }

        Path target = Files.createTempDirectory(build.toPath(), "incremental-restore-");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream print = new PrintStream(output, true);
        int status = new RepositoryTool(print, print).run(
                new String[] { "--repo", repositoryFolder.getPath(), "restore", "latest", target.toString(),
                        "--world-only" });
        assertEquals(output.toString(), 0, status);
        String relative = FileUtils.getRelativePath(this.world);
        for (String file : new String[] { "level.dat", "region/r.0.0.mca", "data/villages.dat" }) {
            assertArrayEquals(
                    file,
                    Files.readAllBytes(world.resolve(file)),
                    Files.readAllBytes(target.resolve(relative).resolve(file)));
        }
    }

    @Test
    public void customNamesNeedOverwriteToBeReplaced() throws Exception {
        long now = System.currentTimeMillis();
        backup("checkpoint", now - 1000, false);
        BackupRoutine taken = new BackupRoutine(universe, options("checkpoint", now, false));
        assertThrows(IllegalArgumentException.class, taken::verifyBackupName);
        backup("checkpoint", now, true);
        try (Repository repository = open()) {
            assertEquals(Collections.singletonList("checkpoint"), repository.archiveNames());
            ArchiveInfo info = repository.readInfo("checkpoint");
            assertEquals(now, info.created());
            assertTrue(info.isCustomName());
        }
        // The ZIP folder is not touched by incremental backups.
        assertFalse(new File(backups, "checkpoint.zip").exists());
    }

    @Test
    public void retentionPrunesTheRepositoryAndFreesUnsharedData() throws Exception {
        long now = System.currentTimeMillis();
        for (int i = 3; i >= 1; i--) {
            // Each backup has a different region, so deleting one frees its data.
            Files.write(world.toPath().resolve("region/r.0.0.mca"), region(100 + i));
            age();
            backup("", now - TimeUnit.HOURS.toMillis(i), false);
        }

        BackupRetention.Plan<? extends BackupEntry> byPolicy = retention(true, 0, 0, "90m:all").preview().get(0);
        assertEquals(2, byPolicy.delete.size());
        assertTrue("Freeing two regions", byPolicy.deletedSize > 2 * 390_000);

        BackupRetention.Plan<? extends BackupEntry> bySize = retention(true, 1, 0, "1d:all").preview().get(0);
        assertEquals("Only the latest backup is always kept", 2, bySize.delete.size());
        assertTrue(bySize.remainingRotationSize > 1);

        long before;
        try (Repository repository = open()) {
            before = repository.diskSize();
        }
        BackupResult.Retention result = retention(true, 0, 2).prune();
        assertNull(result.error);
        assertEquals(1, result.filesRemoved);
        assertTrue("Freeing one region", result.bytesRemoved > 390_000);
        try (Repository repository = open()) {
            assertEquals(2, repository.archiveNames().size());
            assertTrue("Compacted", repository.diskSize() < before - 390_000);
            assertTrue(repository.check(true, message -> {}).ok());
        }
    }

    @Test
    public void sizeAllowanceOnlyCountsWhatRotatingArchivesAddToProtectedOnes() throws Exception {
        long now = System.currentTimeMillis();
        backup("pinned", now - TimeUnit.HOURS.toMillis(3), false);
        Path region = world.toPath().resolve("region/r.0.0.mca");
        for (int i = 2; i >= 1; i--) {
            byte[] bytes = Files.readAllBytes(region);
            System.arraycopy(random(4096, i), 0, bytes, 100_000 * i, 4096);
            Files.write(region, bytes);
            age();
            backup("", now - TimeUnit.HOURS.toMillis(i), false);
        }

        // The rotating archives share the region with the protected custom one, and only add their changes on top.
        BackupRetention.Plan<? extends BackupEntry> plan = retention(false, 100_000, 0, "1d:all").preview().get(0);
        assertTrue(plan.delete.isEmpty());
        assertTrue(plan.remainingRotationSize < 100_000);
        assertTrue("The repository holds the whole region", plan.remainingSize > 390_000);
    }

    @Test
    public void listingAndPreviewIncludeIncrementalArchives() throws Exception {
        long now = System.currentTimeMillis();
        backup("", now - 1000, false);
        List<? extends BackupEntry> entries = RetentionRoutine.fromConfig().list();
        List<RepositoryStore.Entry> incremental = entries.stream().filter(RepositoryStore.Entry.class::isInstance)
                .map(RepositoryStore.Entry.class::cast).collect(Collectors.toList());
        assertEquals(1, incremental.size());
        assertEquals(now - 1000, incremental.get(0).createdAt());

        List<BackupRetention.Plan<? extends BackupEntry>> plans = retention(true, 0, 0, "1h:all").preview();
        assertTrue(plans.get(0).keep.containsKey(incremental.get(0)));
    }
}
