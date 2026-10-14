package serverutils.task.backup;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.RegionFileCache;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.WorldRegion;
import serverutils.lib.util.compression.ICompress;

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
    public void save_WhenPreroutineFails_RestoresWorldState() throws MinecraftException {
        Exception exception = new MinecraftException("An error occurred");
        doThrow(exception).when(universe).disableWorldSaving();

        BackupResult.Save result = routine.save();
        assertEquals(exception, result.error);

        verify(universe, times(1)).restoreWorldSaving();
    }

    @Test
    public void save_WhenUniverseFails_ThrowsException() throws IOException {
        IOException exception = new IOException("An error occurred");
        doThrow(exception).when(universe).saveForBackup();

        BackupResult.Save result = routine.save();
        assertEquals(exception, result.error);

        verify(universe, times(1)).restoreWorldSaving();
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
