package serverutils.task.backup;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import net.minecraft.world.MinecraftException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import serverutils.lib.util.FileUtils;
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

    private Path worldPath() {
        return world.toPath();
    }

    @Test
    public void nestedWorldBackupsParticipateInBothRetentionModes() throws Exception {
        net.minecraft.nbt.NBTTagCompound universe = new net.minecraft.nbt.NBTTagCompound();
        Path universeFile = Files.createDirectories(worldPath().resolve("serverutilities")).resolve("universe.dat");
        try (java.io.OutputStream output = Files.newOutputStream(universeFile)) {
            net.minecraft.nbt.CompressedStreamTools.writeCompressed(universe, output);
        }

        File old = new File(backups, "nested-legacy.zip");
        File latest = new File(backups, "nested-modern.zip");

        try {
            for (File archive : new File[] { old, latest }) {
                options.customName = FileUtils.getBaseName(archive);

                BackupResult.Save result = routine.save();
                assertNull(result.error);
                assertEquals(2, result.newFiles);
                assertEquals(archive.getAbsoluteFile(), result.destination.getAbsoluteFile());
                assertEquals(world.getAbsoluteFile(), result.world.getAbsoluteFile());
                assertEquals(0, result.unmodifiedChunks);
                assertEquals(0, result.unmodifiedRegions);
                assertEquals(0, result.chunks);
            }

            Files.setLastModifiedTime(old.toPath(), java.nio.file.attribute.FileTime.fromMillis(1));
            ArchiveStore archiveStore = new ArchiveStore(backups);
            List<ArchiveEntry> entries = archiveStore.list();
            assertTrue(
                    entries.stream().map(e -> e.file).collect(java.util.stream.Collectors.toSet())
                            .containsAll(Arrays.asList(old, latest)));
        } finally {
            Files.deleteIfExists(old.toPath());
            Files.deleteIfExists(latest.toPath());
        }
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

    private static class CompatibleSaver {

        public static final CompatibleSaver INSTANCE = new CompatibleSaver();
        Exception failure;

        public void flush() throws Exception {
            if (failure != null) throw failure;
        }
    }

}
