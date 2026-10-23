package serverutils.task.backup;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraftforge.common.DimensionManager;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.WorldRegion;
import serverutils.lib.util.compression.CommonsCompressor;
import serverutils.lib.util.compression.ICompress;
import serverutils.lib.util.compression.LegacyCompressor;

public class BackupRoutineTest extends BackupFixtures.Tests {

    private BackupRoutine routine;
    private ICompress compressor;
    private BackupRoutine.Options options;

    @Before
    public void beforeEach() {
        testSetup();
        compressor = mock(ICompress.class);

        when(universe.getUUID()).thenReturn(BackupFixtures.UUID);

        options = BackupRoutine.Options.fromConfig();
        options.backupsDirectory = backups;
        routine = new BackupRoutine(universe, options, () -> compressor);
    }

    @After
    public void afterEach() {
        testReset();
    }

    @Test
    public void verifyBackupName_WhenNameTaken_RequiresOverwrite() throws IOException {
        Files.write(new File(backups, "taken.zip").toPath(), new byte[] { 1 });
        options.customName = "taken";

        assertThrows(IllegalArgumentException.class, routine::verifyBackupName);
    }

    @Test
    public void verifyBackupName_WhenOverwritingTakenName_Accepts() throws IOException {
        Files.write(new File(backups, "taken.zip").toPath(), new byte[] { 1 });
        options.customName = "taken";
        options.overwrite = true;

        routine.verifyBackupName();
    }

    @Test
    public void verifyBackupName_WhenOverwriteWithoutName_Throws() {
        options.overwrite = true;

        assertThrows(IllegalArgumentException.class, routine::verifyBackupName);
    }

    @Test
    public void save_AutomaticBackupName_NeverConflicts() {
        Set<String> usedNames = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            BackupResult.Save result = routine.save();
            String name = result.destination.getName();
            assertTrue(ArchiveStore.LEGACY_NAME.matcher(name).matches());
            assertTrue(usedNames.add(name));
        }
    }

    @Test
    public void save_AutomaticBackupName_MatchesMetadataCreationTime() throws IOException {
        routine = new BackupRoutine(universe, options);
        BackupResult.Save result = routine.save();
        File destination = result.destination;
        ArchiveEntry entry = ArchiveStore.read(destination, FileUtils.getSize(destination));
        LocalDateTime time = LocalDateTime
                .parse(destination.getName().replace(".zip", ""), BackupRoutine.DATE_FORMATTER);
        // files were never saved in millis, metadata is- so divide by 1,000 to match precision (to second)
        assertEquals(entry.createdAt() / 1000, time.atZone(ZoneId.systemDefault()).toEpochSecond());
    }

    @Test
    public void save_WhenPreroutineFails_RestoresWorldState() throws MinecraftException {
        WorldServer worldServer = server.worldServers[0];
        worldServer.levelSaving = true;
        Exception exception = new MinecraftException("An error occurred");
        doThrow(exception).when(worldServer).saveAllChunks(true, null);

        BackupResult.Save result = routine.save();
        assertEquals(exception, result.error);

        assertFalse(BackupRoutine.isWorldSavingSuspended());
        assertTrue(worldServer.levelSaving);
    }

    @Test
    public void save_WhenUniverseFails_ThrowsException() throws IOException {
        IOException exception = new IOException("An error occurred");
        doThrow(exception).when(universe).saveForBackup();

        BackupResult.Save result = routine.save();
        assertEquals(exception, result.error);

        assertFalse(BackupRoutine.isWorldSavingSuspended());
    }

    @Test
    public void optionalSaverPropagatesFailuresAndAllowsOlderVersions() throws Exception {
        CompatibleSaver saver = CompatibleSaver.INSTANCE;
        try {
            saver.failure = null;
            BackupRoutine.flushWorldDataSaver(CompatibleSaver.class);
            IOException diskError = new IOException("disk failure");
            saver.failure = diskError;
            assertSame(
                    diskError,
                    assertThrows(IOException.class, () -> BackupRoutine.flushWorldDataSaver(CompatibleSaver.class)));
            saver.failure = new InterruptedException("cancelled");
            assertThrows(InterruptedException.class, () -> BackupRoutine.flushWorldDataSaver(CompatibleSaver.class));
            BackupRoutine.flushWorldDataSaver(Object.class); // Older saver has no flush API.
        } finally {
            saver.failure = null;
        }
    }

    @Test
    public void flushChunkSaves_DrainsStrandedWorkAndRestoresFlags() throws Exception {
        WorldServer worldServer = mock(WorldServer.class);
        worldServer.levelSaving = true;
        File regionRoot = FileUtils.ensureExists(new File(build, "stranded"));
        StallingLoader loader = new StallingLoader(regionRoot);
        doAnswer(invocation -> {
            assertFalse("Saving must be enabled while flushing", worldServer.levelSaving);
            loader.saveExtraData();
            return null;
        }).when(worldServer).saveChunkData();

        try {
            // Chunk 1 is queued after the IO thread found the loader idle, so only saveChunkData can drain it.
            loader.enqueue(0);
            try {
                assertTrue(loader.idle.await(10, TimeUnit.SECONDS));
                loader.enqueue(1);
            } finally {
                loader.release.countDown();
            }

            BackupRoutine.flushChunkSaves(new WorldServer[] { null, worldServer });

            assertTrue(worldServer.levelSaving);
            try (DataInputStream in = RegionFileCache.getChunkInputStream(regionRoot, 1, 0)) {
                assertNotNull("Stranded chunk must reach disk", in);
                assertEquals(1, CompressedStreamTools.read(in).getInteger("value"));
            }
        } finally {
            loader.release.countDown();
            RegionFileCache.clearRegionFileReferences();
        }
    }

    @Test
    public void flushChunkSaves_WhenFlushFails_RestoresFlags() {
        WorldServer worldServer = mock(WorldServer.class);
        worldServer.levelSaving = true;
        doThrow(new IllegalStateException("flush failed")).when(worldServer).saveChunkData();

        assertThrows(
                IllegalStateException.class,
                () -> BackupRoutine.flushChunkSaves(new WorldServer[] { worldServer }));
        assertTrue(worldServer.levelSaving);
    }

    @Test
    public void claimOnly_KeepsMiscFiles() throws IOException {
        routine = new BackupRoutine(universe, options);
        options.onlyClaimed = true;
        BackupResult.Save result = routine.save();
        try (ZipFile zip = new ZipFile(result.destination)) {
            assertNotNull(zip.getEntry(FileUtils.getRelativePath(new File(world, "level.dat"))));
        }
    }

    @Test
    public void claimOnly_DropsUnclaimedRegions() throws Exception {
        routine = new BackupRoutine(universe, options);
        WorldFile claimed = WorldFile.from(BackupCandidates.OVERWORLD, world, "region/r.0.0.mca");
        WorldFile unclaimed = WorldFile.from(BackupCandidates.OVERWORLD, world, "region/r.1.1.mca");

        writeToRegion(claimed, 0, 0, "test");
        writeToRegion(unclaimed, 0, 0, "test");

        claim(new ChunkDimPos(0, 0, 0));

        options.onlyClaimed = true;

        BackupResult.Save result = routine.save();
        try (ZipFile zip = new ZipFile(result.destination)) {
            assertNotNull(zip.getEntry(claimed.runtimeRelativePath()));
            assertNull(zip.getEntry(unclaimed.runtimeRelativePath()));
        }
    }

    @Test
    public void claimOnly_WhenChunkOnlyCopy_TrimsRegionToClaimedChunks() throws IOException {
        options.writeOnlyClaimedChunks = true;
        options.onlyClaimed = true;
        routine = new BackupRoutine(universe, options);

        WorldFile region = WorldFile.from(BackupCandidates.OVERWORLD, world, "region/r.0.0.mca");
        WorldFile unclaimed = WorldFile.from(BackupCandidates.OVERWORLD, world, "region/r.1.1.mca");

        writeToRegion(region, 0, 0, "test0.0");
        writeToRegion(unclaimed, 0, 0, "test0.0");
        writeToRegion(region, 1, 1, "test1.1");

        claim(new ChunkDimPos(0, 0, 0));

        BackupResult.Save result = routine.save();
        try (ZipFile zip = new ZipFile(result.destination)) {
            ZipEntry regionEntry = zip.getEntry(region.runtimeRelativePath());
            assertNotNull(regionEntry);

            WorldRegion checkedRegion = uncompressRegion(zip, regionEntry, BackupCandidates.OVERWORLD);
            checkedRegion.read(0, 0, tag -> assertEquals("test0.0", tag.getString("value")));
            checkedRegion.read(1, 1, tag -> assertNull(tag.getString("value")));

            ZipEntry unclaimedEntry = zip.getEntry(unclaimed.runtimeRelativePath());
            assertNull(unclaimedEntry);
        }
    }

    @Test
    public void claimOnly_WhenCopyFullRegion_SavesEntireRegionWithClaim() throws IOException {
        options.writeOnlyClaimedChunks = false;
        options.onlyClaimed = true;
        routine = new BackupRoutine(universe, options);

        WorldFile region1 = WorldFile.from(BackupCandidates.OVERWORLD, world, "region/r.0.0.mca");
        WorldFile region2 = WorldFile.from(BackupCandidates.OVERWORLD, world, "region/r.1.1.mca");

        writeToRegion(region1, 0, 0, "test0.0");
        writeToRegion(region2, 0, 0, "test0.0");
        writeToRegion(region1, 1, 1, "test1.1");

        claim(new ChunkDimPos(0, 0, 0), new ChunkDimPos(1 << 5, 1 << 5, 0));

        BackupResult.Save result = routine.save();
        try (ZipFile zip = new ZipFile(result.destination)) {
            ZipEntry region1Entry = zip.getEntry(region1.runtimeRelativePath());
            assertNotNull(region1Entry);

            WorldRegion checkedRegion1 = uncompressRegion(zip, region1Entry, BackupCandidates.OVERWORLD);
            checkedRegion1.read(0, 0, tag -> assertEquals("test0.0", tag.getString("value")));
            checkedRegion1.read(1, 1, tag -> assertEquals("test1.1", tag.getString("value")));

            ZipEntry region2Entry = zip.getEntry(region2.runtimeRelativePath());
            assertNotNull(region2Entry);

            WorldRegion checkedRegion2 = uncompressRegion(zip, region2Entry, BackupCandidates.OVERWORLD);
            checkedRegion2.read(0, 0, tag -> assertEquals("test0.0", tag.getString("value")));
        }
    }

    @Test
    public void claimOnly_NumberedDimension_MatchesDimFolder() throws IOException {
        options.writeOnlyClaimedChunks = true;
        options.onlyClaimed = true;
        routine = new BackupRoutine(universe, options);

        WorldFile claimed = worldFile("DIM7", "region/r.-1.-1.mca");
        WorldFile unclaimed = worldFile("DIM7", "region/r.0.0.mca");

        writeToRegion(claimed, 31, 31, "claimed");
        writeToRegion(claimed, 30, 31, "unclaimed");
        writeToRegion(unclaimed, 0, 0, "unclaimed");

        claim(new ChunkDimPos(-1, -1, 7));

        BackupResult.Save result = routine.save();
        try (ZipFile zip = new ZipFile(result.destination)) {
            assertEquals("claimed", readValue(zip, claimed, 31, 31));
            assertNull(readValue(zip, claimed, 30, 31));
            assertNull(zip.getEntry(unclaimed.runtimeRelativePath()));
        }
    }

    @Test
    public void claimOnly_NumberedDimensionWithoutClaims_DropsRegions() throws IOException {
        // The old backup preserved DIM folders whose provider could not resolve its save folder. Dimensions are now
        // matched by folder name, so an unclaimed DIM folder is dropped like any other unclaimed region.
        options.onlyClaimed = true;
        routine = new BackupRoutine(universe, options);

        WorldFile region = worldFile("DIM9", "region/r.0.0.mca");
        writeToRegion(region, 0, 0, "unclaimed");

        claim(new ChunkDimPos(0, 0, 0));

        BackupResult.Save result = routine.save();
        try (ZipFile zip = new ZipFile(result.destination)) {
            assertNull(zip.getEntry(region.runtimeRelativePath()));
        }
    }

    @Test
    public void claimOnly_UnknownDimensionFolder_CopiedUnchanged() throws IOException {
        options.writeOnlyClaimedChunks = true;
        options.onlyClaimed = true;
        routine = new BackupRoutine(universe, options);

        WorldFile custom = worldFile("custom-moon", "region/r.-1.-1.mca");
        WorldFile removed = worldFile("removed-mod", "region/r.0.0.mca");
        writeToRegion(custom, 31, 31, "custom");
        writeToRegion(custom, 30, 31, "unclaimed");
        writeToRegion(removed, 0, 0, "removed");

        claim(new ChunkDimPos(-1, -1, 8));

        BackupResult.Save result = routine.save();
        try (ZipFile zip = new ZipFile(result.destination)) {
            assertArchivedUnchanged(zip, custom);
            assertArchivedUnchanged(zip, removed);
        }
    }

    @Test
    public void claimOnly_RegisteredCustomSaveFolder_TrimmedToClaims() throws IOException {
        options.writeOnlyClaimedChunks = true;
        options.onlyClaimed = true;
        routine = new BackupRoutine(universe, options);

        WorldFile claimed = worldFile("custom-moon", "region/r.-1.-1.mca");
        WorldFile unclaimed = worldFile("custom-moon", "region/r.0.0.mca");
        writeToRegion(claimed, 31, 31, "claimed");
        writeToRegion(claimed, 30, 31, "unclaimed");
        writeToRegion(unclaimed, 0, 0, "unclaimed");

        claim(new ChunkDimPos(-1, -1, 8));

        BackupResult.Save result = saveWithDimensionFolder(8, "custom-moon");
        try (ZipFile zip = new ZipFile(result.destination)) {
            assertEquals("claimed", readValue(zip, claimed, 31, 31));
            assertNull(readValue(zip, claimed, 30, 31));
            assertNull(zip.getEntry(unclaimed.runtimeRelativePath()));
        }
    }

    @Test
    public void claimOnly_RegisteredCustomSaveFolderWithoutClaims_DropsRegions() throws IOException {
        options.onlyClaimed = true;
        routine = new BackupRoutine(universe, options);

        WorldFile region = worldFile("custom-moon", "region/r.0.0.mca");
        writeToRegion(region, 0, 0, "unclaimed");

        claim(new ChunkDimPos(0, 0, 0));

        BackupResult.Save result = saveWithDimensionFolder(8, "custom-moon");
        try (ZipFile zip = new ZipFile(result.destination)) {
            assertNull(zip.getEntry(region.runtimeRelativePath()));
        }
    }

    private BackupResult.Save saveWithDimensionFolder(int dimension, String folder) {
        WorldProvider provider = mock(WorldProvider.class);
        when(provider.getSaveFolder()).thenReturn(folder);
        try (MockedStatic<DimensionManager> dimensions = mockStatic(DimensionManager.class)) {
            dimensions.when(DimensionManager::getStaticDimensionIDs).thenReturn(new Integer[] { dimension });
            dimensions.when(() -> DimensionManager.createProviderFor(dimension)).thenReturn(provider);
            return routine.save();
        }
    }

    @Test
    public void claimOnly_NestedRegionFolder_CopiedUnchanged() throws IOException {
        options.writeOnlyClaimedChunks = true;
        options.onlyClaimed = true;
        routine = new BackupRoutine(universe, options);

        WorldFile nested = worldFile(BackupCandidates.OVERWORLD, "region/unowned/r.0.0.mca");
        writeToRegion(nested, 0, 0, "nested");
        writeToRegion(nested, 1, 1, "unclaimed");
        assertEquals(WorldFile.Kind.MISC, nested.kind());

        claim(new ChunkDimPos(0, 0, 0));

        BackupResult.Save result = routine.save();
        try (ZipFile zip = new ZipFile(result.destination)) {
            assertArchivedUnchanged(zip, nested);
        }
    }

    @Test
    public void claimOnly_MalformedRegion_CopiedUnchangedAndSourceUntouched() throws IOException {
        options.writeOnlyClaimedChunks = true;
        options.onlyClaimed = true;
        routine = new BackupRoutine(universe, options);

        WorldFile malformed = worldFile(BackupCandidates.OVERWORLD, "region/r.2.0.mca");
        FileUtils.ensureExists(malformed.backing().getParentFile());
        claim(new ChunkDimPos(64, 0, 0));

        // Empty, a header with no sector table, and a size that is not sector aligned
        for (int size : new int[] { 0, 4096, 8193 }) {
            byte[] original = new byte[size];
            Arrays.fill(original, (byte) 42);
            Files.write(malformed.backing().toPath(), original);

            BackupResult.Save result = routine.save();
            assertArrayEquals(
                    "source modified at size " + size,
                    original,
                    Files.readAllBytes(malformed.backing().toPath()));
            try (ZipFile zip = new ZipFile(result.destination)) {
                assertArchivedUnchanged(zip, malformed);
            }
        }
    }

    @Test
    public void claimOnly_NoClaims_OmitsNumberedDimensionRegions() throws IOException {
        options.onlyClaimed = true;
        routine = new BackupRoutine(universe, options);

        WorldFile overworld = worldFile(BackupCandidates.OVERWORLD, "region/r.0.0.mca");
        WorldFile dim7 = worldFile("DIM7", "region/r.0.0.mca");
        WorldFile custom = worldFile("custom-moon", "region/r.0.0.mca");
        writeToRegion(overworld, 0, 0, "overworld");
        writeToRegion(dim7, 0, 0, "dim7");
        writeToRegion(custom, 0, 0, "custom");

        claim();

        BackupResult.Save result = routine.save();
        try (ZipFile zip = new ZipFile(result.destination)) {
            assertNotNull(zip.getEntry(FileUtils.getRelativePath(new File(world, "level.dat"))));
            assertNull(zip.getEntry(overworld.runtimeRelativePath()));
            assertNull(zip.getEntry(dim7.runtimeRelativePath()));
            assertArchivedUnchanged(zip, custom);
        }
    }

    @Test
    public void snapshotFiles_ShouldCopyFileOnMainThreadBeforeSave() throws Exception {
        options.snapshotMetaFiles = true;
        options.useSeparateThread = true;
        File metadataFile = new File(world, "level.dat");

        long currentThreadId = Thread.currentThread().getId();

        BackupCandidates candidateMock = mock(BackupCandidates.class);
        when(candidateMock.getFiles(any(Predicate.class))).thenAnswer(invocation -> {
            // assert that the thread reading the files is on the calling thread (main thread)
            long threadId = Thread.currentThread().getId();
            assertEquals(currentThreadId, threadId);
            return Collections.singletonList(WorldFile.from(BackupCandidates.OVERWORLD, world, metadataFile));
        });

        ExecutorService otherThread = Executors.newSingleThreadExecutor();

        verifyInSnapshot(() -> {
            // this code runs in the main routine of BackupRoutine and thus should have a different thread id
            assertNotEquals(currentThreadId, Thread.currentThread().getId());
            return Collections.singletonList(metadataFile);
        });

        BackupResult.Save result;
        try (MockedStatic<BackupCandidates> candidates = mockStatic(BackupCandidates.class)) {
            candidates.when(() -> BackupCandidates.scanWorld(eq(world), eq(backups), anyBoolean()))
                    .thenReturn(candidateMock);
            result = routine.save(otherThread).get(5, TimeUnit.SECONDS);
        } finally {
            otherThread.shutdown();
        }

        assertNull(result.error);
    }

    @Test
    public void snapshotFiles_WhenFileChangesDuringCopy_FailsBackup() throws Exception {
        options.snapshotMetaFiles = true;

        File changing = new File(world, "level.dat") {

            private boolean read;

            @Override
            public long lastModified() {
                long modified = super.lastModified();
                if (!read) {
                    read = true;
                    return modified;
                }
                return modified + 1000;
            }
        };

        mockCandidates(changing);
        BackupResult.Save result = routine.save();

        assertTrue(result.error instanceof IOException);
        assertTrue(result.error.getMessage().startsWith("File changed during backup snapshot"));
        assertNull(result.destination);
        assertFalse(new File(BackupRoutine.BACKUP_TEMP_FOLDER, "snapshot").exists());
        assertFalse(BackupRoutine.isWorldSavingSuspended());
        verify(compressor, times(0)).addFileToArchive(any(), any());
    }

    @Test
    public void snapshotFiles_WhenFileIsMissing_FailsBackup() throws IOException {
        File deleted = new File(world, "level.dat") {

            @Override
            public long lastModified() {
                long lastModified = super.lastModified();
                FileUtils.delete(this);
                return lastModified;
            }
        };

        mockCandidates(deleted);
        BackupResult.Save result = routine.save();

        assertTrue(result.error instanceof NoSuchFileException);
        assertEquals(deleted.getPath(), result.error.getMessage());
        assertNull(result.destination);
        assertFalse(new File(BackupRoutine.BACKUP_TEMP_FOLDER, "snapshot").exists());
        assertFalse(BackupRoutine.isWorldSavingSuspended());
        verify(compressor, times(0)).addFileToArchive(any(), any());
    }

    @Test
    public void snapshotFiles_WhenStableModDataIsLinked_SnapshotsIt() throws Exception {
        options.snapshotMetaFiles = true;
        options.world = world;

        // AE2/spawndata is normally read live, but only when it is the world's own folder
        File outside = FileUtils.ensureExists(new File(build, "outside"));
        Files.write(new File(outside, "keep.dat").toPath(), new byte[] { 42 });
        File spawndata = new File(FileUtils.ensureExists(new File(world, "AE2")), "spawndata");
        try {
            Files.createSymbolicLink(spawndata.toPath(), outside.toPath().toAbsolutePath());
        } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
            Assume.assumeNoException(unsupported);
        }

        verifyInSnapshot(() -> Collections.singletonList(new File(spawndata, "keep.dat")));
        BackupResult.Save result = routine.save();

        assertNull(result.error);
        verify(compressor, times(2)).close();
    }

    @Test
    public void snapshotFiles_WhenStableModDataIsFileLinkInsideWorld_SnapshotsIt() throws Exception {
        options.snapshotMetaFiles = true;
        options.world = world;

        // The target is the world's own file, but the stable name belongs to the link, not the target
        File target = new File(world, "changing.dat");
        Files.write(target.toPath(), new byte[] { 1, 2, 3 });
        File link = new File(FileUtils.ensureExists(new File(world, "AE2/spawndata")), "0_0_0.dat");
        try {
            Files.createSymbolicLink(link.toPath(), target.toPath().toAbsolutePath());
        } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
            Assume.assumeNoException(unsupported);
        }

        verifyInSnapshot(() -> Arrays.asList(target, link));
        BackupResult.Save result = routine.save();

        assertNull(result.error);
        verify(compressor, times(2)).close();
    }

    @Test
    public void verifyBackupName_WhenNameEscapesStorage_Throws() {
        for (String name : new String[] { "../escape", "..\\escape", new File(build, "escape").getAbsolutePath(),
                "C:escape", "invalid\u0000name" }) {
            options.customName = name;
            BackupRoutine routine = new BackupRoutine(universe, options, () -> compressor);

            assertThrows(name, IllegalArgumentException.class, routine::save);
        }

        assertFalse(new File(build, "escape.zip").exists());
        verify(universe.server.getConfigurationManager(), never()).saveAllPlayerData();
        verifyNoInteractions(compressor);
    }

    @Test
    public void verifyBackupName_WhenDestinationIsDirectory_Throws() throws IOException {
        File directory = FileUtils.ensureExists(new File(backups, "taken.zip"));
        File contents = new File(directory, "keep");
        Files.write(contents.toPath(), new byte[] { 1 });
        options.customName = "taken";

        for (boolean overwrite : new boolean[] { false, true }) {
            options.overwrite = overwrite;
            assertThrows(IllegalArgumentException.class, new BackupRoutine(universe, options)::verifyBackupName);
        }

        assertTrue(contents.isFile());
    }

    @Test
    public void save_WhenNameTakenDuringCompression_KeepsOtherFile() throws Exception {
        options.customName = "checkpoint";
        File checkpoint = new File(backups, "checkpoint.zip");
        byte[] other = { 1, 2, 3 };

        ICompress race = new LegacyCompressor() {

            @Override
            public void close() throws Exception {
                super.close();
                Files.write(checkpoint.toPath(), other);
            }
        };
        BackupResult.Save result = new BackupRoutine(universe, options, () -> race).save();

        assertTrue(result.error instanceof FileAlreadyExistsException);
        assertNull(result.destination);
        assertArrayEquals(other, Files.readAllBytes(checkpoint.toPath()));
        String[] temporaries = backups.list((dir, name) -> name.startsWith(".su-save-"));
        assertEquals(0, temporaries == null ? 0 : temporaries.length);
    }

    @Test
    public void save_ArchivePermissions_OrdinaryWhenNewAndKeptWhenOverwritten() throws IOException {
        Assume.assumeNotNull(Files.getFileAttributeView(backups.toPath(), PosixFileAttributeView.class));
        options.customName = "posix";
        File archive = new File(backups, "posix.zip");

        // a new archive gets the mode of any newly created file, not the private staging mode
        File reference = new File(build, "reference");
        Files.write(reference.toPath(), new byte[] { 1 });
        assertNull(new BackupRoutine(universe, options).save().error);
        assertEquals(
                Files.getPosixFilePermissions(reference.toPath()),
                Files.getPosixFilePermissions(archive.toPath()));

        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-r-----");
        Files.setPosixFilePermissions(archive.toPath(), permissions);
        options.overwrite = true;
        assertNull(new BackupRoutine(universe, options).save().error);
        assertEquals(permissions, Files.getPosixFilePermissions(archive.toPath()));
    }

    @Test
    public void save_WhenArchiveFails_PreservesPreviousBackupAndCleansUp() throws Exception {
        options.snapshotMetaFiles = true;
        options.world = world;
        options.customName = "failure";
        options.overwrite = true;

        File archive = new File(backups, "failure.zip");
        byte[] previous = { 7, 8, 9 };

        for (boolean legacy : new boolean[] { false, true }) {
            for (String failure : new String[] { "missing-live", "cancel", "close" }) {
                String label = (legacy ? "legacy " : "commons ") + failure;

                // Stable mod data is read live while archiving, after the snapshot was taken
                File live = new File(world, "AE2/spawndata/0_0_0.dat");
                FileUtils.ensureExists(live.getParentFile());
                Files.write(live.toPath(), new byte[] { 42 });
                Files.write(archive.toPath(), previous);

                ICompress spy = spy(legacy ? new LegacyCompressor() : new CommonsCompressor());
                if (failure.equals("missing-live")) {
                    doAnswer(call -> {
                        Files.deleteIfExists(live.toPath());
                        return call.callRealMethod();
                    }).when(spy).addFileToArchive(any(), any());
                }
                if (failure.equals("cancel")) {
                    doAnswer(call -> {
                        call.callRealMethod();
                        Thread.currentThread().interrupt();
                        return null;
                    }).when(spy).addFileToArchive(any(), any());
                }
                if (failure.equals("close")) {
                    doAnswer(call -> {
                        call.callRealMethod();
                        throw new IOException("injected close failure");
                    }).when(spy).close();
                }

                BackupResult.Save result;
                try {
                    result = new BackupRoutine(universe, options, () -> spy).save();
                } finally {
                    Thread.interrupted();
                }

                assertNotNull(label, result.error);
                assertNull(label, result.destination);
                assertArrayEquals(label, previous, Files.readAllBytes(archive.toPath()));
                assertFalse(label, new File(BackupRoutine.BACKUP_TEMP_FOLDER, "snapshot").exists());
                String[] temporaries = backups.list((dir, name) -> name.startsWith(".su-save-"));
                assertEquals(label, 0, temporaries == null ? 0 : temporaries.length);
            }
        }
    }

    @Test
    public void snapshotFiles_StableModDataReadLiveAndOtherModDataFromSnapshot() throws Exception {
        options.snapshotMetaFiles = true;
        options.world = world;

        String[] stable = { "AE2/spawndata/0_0_0.dat",
                "betterquesting/backup/3.8.75-GTNH/QuestDatabase_backup_3.8.75-GTNH.json" };
        // Includes near misses of the stable patterns, which must still be snapshotted
        String[] live = { "AE2/compass/0_0_0.dat", "AE2/settings.cfg", "buildcraft/zonemap/world/r0,0.nbt",
                "betterquesting/QuestDatabase.json", "QuestLoot.json", "AE2/spawndata/nested/unrecognized.dat",
                "AE2/spawndata-extra/0_0_0.dat", "betterquesting/backup/3.8.75-GTNH/other.json" };

        int previousLevel = ServerUtilitiesConfig.backups.compression_level;
        try {
            for (boolean legacy : new boolean[] { false, true }) {
                for (boolean claimed : new boolean[] { false, true }) {
                    for (int level : new int[] { 0, 1 }) {
                        String label = (legacy ? "legacy" : "commons") + (claimed ? " claimed" : " whole")
                                + " level "
                                + level;
                        ServerUtilitiesConfig.backups.compression_level = level;
                        options.onlyClaimed = claimed;
                        if (claimed) claim();

                        for (String name : stable) writeNamed(name);
                        for (String name : live) writeNamed(name);

                        // Archiving starts after the snapshot: stable files are read live and pick up this write,
                        // everything else must come from the snapshot and keep its original contents
                        ICompress spy = spy(legacy ? new LegacyCompressor() : new CommonsCompressor());
                        doAnswer(call -> {
                            for (String name : stable) Files.write(new File(world, name).toPath(), new byte[] { 99 });
                            for (String name : live) Files.write(new File(world, name).toPath(), new byte[] { 99 });
                            return call.callRealMethod();
                        }).when(spy).createOutputStream(any());

                        BackupResult.Save result = new BackupRoutine(universe, options, () -> spy).save();
                        assertNull(label, result.error);

                        try (ZipFile zip = new ZipFile(result.destination)) {
                            for (String name : stable) assertArchived(zip, label, name, new byte[] { 99 });
                            for (String name : live)
                                assertArchived(zip, label, name, name.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                }
            }
        } finally {
            ServerUtilitiesConfig.backups.compression_level = previousLevel;
        }
    }

    private void assertArchived(ZipFile zip, String label, String name, byte[] expected) throws IOException {
        ZipEntry entry = zip.getEntry(FileUtils.getRelativePath(new File(world, name)));
        assertNotNull(label + ": " + name, entry);
        try (InputStream in = zip.getInputStream(entry)) {
            assertArrayEquals(label + ": " + name, expected, IOUtils.toByteArray(in));
        }
    }

    private void writeNamed(String name) throws IOException {
        File file = new File(world, name);
        FileUtils.ensureExists(file.getParentFile());
        Files.write(file.toPath(), name.getBytes(StandardCharsets.UTF_8));
    }

    @Test // to be honest, I don't fully understand this test
    public void snapshotFiles_WhenAppendWriterOpen_CapturesCurrentContents() throws IOException {
        options.snapshotMetaFiles = true;
        routine = new BackupRoutine(universe, options);

        File payload = new File(world, "mod-data.dat");
        byte[] expected = new byte[4097];
        new Random(23).nextBytes(expected);
        Files.write(payload.toPath(), new byte[] { expected[0] });
        mockCandidates(payload);

        // NTFS can still report the original one-byte size until this writer closes
        BackupResult.Save result;
        try (FileOutputStream writer = new FileOutputStream(payload, true)) {
            writer.write(expected, 1, expected.length - 1);
            writer.flush();
            result = routine.save();
        }

        assertNull(result.error);
        try (ZipFile zip = new ZipFile(result.destination)) {
            ZipEntry entry = zip.getEntry(FileUtils.getRelativePath(payload));
            assertNotNull(entry);
            try (InputStream in = zip.getInputStream(entry)) {
                assertArrayEquals(expected, IOUtils.toByteArray(in));
            }
        }
    }

    // compressor is our hook for testing the routine before it closes and clears, it's a bit gross but oh well
    private void verifyInSnapshot(Supplier<List<File>> files) {
        try {
            doAnswer(i -> {
                File snapshotFolder = new File(BackupRoutine.BACKUP_TEMP_FOLDER, "snapshot");
                assertTrue(snapshotFolder.exists());

                Path worldPath = world.toPath().toAbsolutePath().normalize();
                for (File f : files.get()) {
                    Path relative = worldPath.relativize(f.toPath().toAbsolutePath().normalize());
                    assertTrue(new File(snapshotFolder, relative.toString()).exists());
                }
                return null;
            }).when(compressor).close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Pauses the IO thread the first time it finds this loader idle, so the test can queue work **/
    private static class StallingLoader extends AnvilChunkLoader {

        final CountDownLatch idle = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        private boolean paused;

        StallingLoader(File root) {
            super(root);
        }

        void enqueue(int chunkX) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("value", chunkX);
            addChunkToPending(new ChunkCoordIntPair(chunkX, 0), tag);
        }

        @Override
        public boolean writeNextIO() {
            boolean more = super.writeNextIO();
            if (!more && !paused) {
                paused = true;
                idle.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Producer timed out");
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
            return more;
        }
    }

    private static class CompatibleSaver {

        public static final CompatibleSaver INSTANCE = new CompatibleSaver();
        Exception failure;

        public void flush() throws Exception {
            if (failure != null) throw failure;
        }
    }

    private WorldRegion uncompressRegion(ZipFile zip, ZipEntry entry, String dimension) throws IOException {
        File temp = FileUtils.ensureExists(new File(build, "temp"));
        File dest = Files.createTempFile(temp.toPath(), "claimed-region-", ".mca").toFile();

        try (InputStream in = zip.getInputStream(entry)) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new WorldRegion(dest, dimension);
    }

    private WorldFile worldFile(String dimension, String path) {
        File root = BackupCandidates.OVERWORLD.equals(dimension) ? world : new File(world, dimension);
        return WorldFile.from(dimension, root, path);
    }

    private String readValue(ZipFile zip, WorldFile file, int chunkX, int chunkZ) throws IOException {
        ZipEntry entry = zip.getEntry(file.runtimeRelativePath());
        assertNotNull(entry);
        String[] value = new String[1];
        try (WorldRegion region = uncompressRegion(zip, entry, file.dimension)) {
            region.read(chunkX, chunkZ, tag -> value[0] = tag.getString("value"));
        }
        return value[0];
    }

    private static void assertArchivedUnchanged(ZipFile zip, WorldFile file) throws IOException {
        ZipEntry entry = zip.getEntry(file.runtimeRelativePath());
        assertNotNull(file.runtimeRelativePath() + " missing from archive", entry);
        try (InputStream in = zip.getInputStream(entry)) {
            assertArrayEquals(Files.readAllBytes(file.backing().toPath()), IOUtils.toByteArray(in));
        }
    }

    private void writeToRegion(WorldFile file, int chunkX, int chunkY, String value) throws IOException {
        FileUtils.ensureExists(file.backing().getParentFile());

        try (WorldRegion region = new WorldRegion(file)) {
            region.write(chunkX, chunkY, () -> {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("value", value);
                return tag;
            });
        }
    }
}
