package serverutils.task.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.SaveHandler;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.data.Universe;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.compression.ICompress;

public class BackupTaskTest {

    @BeforeClass
    public static void configureBackups() {
        ServerUtilitiesConfig.backups.backup_folder_path = "build/test-backups";
        ServerUtilitiesConfig.backups.additional_backup_files = new String[0];
        ServerUtilitiesConfig.backups.backups_to_keep = 12;
        ServerUtilitiesConfig.backups.compression_level = 1;
        ServerUtilitiesConfig.backups.enable_backups = true;
        ServerUtilitiesConfig.backups.need_online_players = false;
        ServerUtilitiesConfig.backups.only_backup_claimed_chunks = false;
        ServerUtilitiesConfig.backups.silent_backup = true;
        ServerUtilitiesConfig.backups.use_separate_thread = true;
    }

    @AfterClass
    public static void removeTestBackups() {
        FileUtils.delete(BackupTask.BACKUP_FOLDER);
    }

    @After
    public void resetBackupState() {
        BackupTask.stopBackupThread();
    }

    @Test
    public void protectsNewWorldsAndRejectsSaveCommandsUntilCleanup() throws Exception {
        WorldServer world = mock(WorldServer.class);
        WorldServer loadedLater = mock(WorldServer.class);
        BackupTask.saveAndDisableWorldSaving(new WorldServer[] { world });
        serverutils.handlers.ServerUtilitiesServerEventHandler
                .loadWorldEvent(new net.minecraftforge.event.world.WorldEvent.Load(loadedLater));
        assertTrue(loadedLater.levelSaving);
        for (net.minecraft.command.ICommand command : new net.minecraft.command.ICommand[] {
                new net.minecraft.command.server.CommandSaveAll(), new net.minecraft.command.server.CommandSaveOn(),
                new net.minecraft.command.server.CommandSaveOff() }) {
            net.minecraftforge.event.CommandEvent event = org.mockito.Mockito
                    .spy(new net.minecraftforge.event.CommandEvent(command, mock(ICommandSender.class), new String[0]));
            // Forge adds this override from @Cancelable when transforming CommandEvent at runtime.
            when(event.isCancelable()).thenReturn(true);
            serverutils.handlers.ServerUtilitiesServerEventHandler.onBackupSaveCommand(event);
            assertTrue(event.isCanceled());
            assertTrue(event.exception instanceof net.minecraft.command.CommandException);
        }
        BackupTask.stopBackupThread();
        assertFalse(world.levelSaving);
        assertFalse(loadedLater.levelSaving);
        net.minecraftforge.event.CommandEvent event = new net.minecraftforge.event.CommandEvent(
                new net.minecraft.command.server.CommandSaveAll(),
                mock(ICommandSender.class),
                new String[0]);
        serverutils.handlers.ServerUtilitiesServerEventHandler.onBackupSaveCommand(event);
        assertFalse(event.isCanceled());
    }

    @Test
    public void backupSavesPreviouslyDisabledWorldAndRestoresItsFlag() throws Exception {
        WorldServer world = mock(WorldServer.class);
        world.levelSaving = true;
        doAnswer(invocation -> {
            assertFalse("Backup must force a current chunk snapshot", world.levelSaving);
            return null;
        }).when(world).saveAllChunks(true, null);
        BackupTask.saveAndDisableWorldSaving(new WorldServer[] { world });
        BackupTask.restoreWorldSaving();
        assertTrue(world.levelSaving);
    }

    @Test
    public void forcedClaimFilteringWorksWithConfigOffAndWithNoClaims() throws Exception {
        File source = new File("build/test-claimed-backup-source");
        File regions = new File(source, "region");
        assertTrue(regions.mkdirs() || regions.isDirectory());
        File claimed = new File(regions, "r.0.0.mca");
        File unclaimed = new File(regions, "r.1.0.mca");
        Files.write(claimed.toPath(), new byte[] { 1 });
        Files.write(unclaimed.toPath(), new byte[] { 2 });
        WorldServer world = mock(WorldServer.class);
        Field provider = net.minecraft.world.World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(world, mock(net.minecraft.world.WorldProvider.class));
        when(world.getChunkSaveLocation()).thenReturn(source);
        MinecraftServer server = mock(MinecraftServer.class);
        server.worldServers = new WorldServer[] { world };
        ISaveFormat saveFormat = mock(ISaveFormat.class);
        SaveHandler saveHandler = mock(SaveHandler.class);
        when(server.getActiveAnvilConverter()).thenReturn(saveFormat);
        when(saveFormat.getSaveLoader(server.getFolderName(), false)).thenReturn(saveHandler);
        when(saveHandler.getWorldDirectory()).thenReturn(source);
        setCurrentServer(server);
        ServerUtilitiesConfig.backups.only_backup_claimed_chunks = false;
        ServerUtilitiesConfig.backups.backup_entire_regions_with_claims = true;
        Field loaderInstance = cpw.mods.fml.common.Loader.class.getDeclaredField("instance");
        loaderInstance.setAccessible(true);
        Object previousLoader = loaderInstance.get(null);
        loaderInstance.set(null, mock(cpw.mods.fml.common.Loader.class));
        cpw.mods.fml.common.FMLCommonHandler fml = cpw.mods.fml.common.FMLCommonHandler.instance();
        Field sidedDelegate = cpw.mods.fml.common.FMLCommonHandler.class.getDeclaredField("sidedDelegate");
        sidedDelegate.setAccessible(true);
        Object previousDelegate = sidedDelegate.get(fml);
        cpw.mods.fml.common.IFMLSidedHandler side = mock(cpw.mods.fml.common.IFMLSidedHandler.class);
        when(side.getServer()).thenReturn(server);
        sidedDelegate.set(fml, side);
        try {
            for (boolean empty : new boolean[] { false, true }) {
                java.util.Set<serverutils.lib.math.ChunkDimPos> claims = empty ? Collections.emptySet()
                        : Collections.singleton(new serverutils.lib.math.ChunkDimPos(0, 0, 0));
                ThreadBackup.doBackup(ICompress.createCompressor(), source, "forced-claims", claims, null, true);
                try (ZipFile zip = new ZipFile(new File(BackupTask.BACKUP_FOLDER, "forced-claims.zip"))) {
                    assertEquals(!empty, zip.getEntry(FileUtils.getRelativePath(claimed)) != null);
                    assertNull(zip.getEntry(FileUtils.getRelativePath(unclaimed)));
                }
            }
        } finally {
            sidedDelegate.set(fml, previousDelegate);
            loaderInstance.set(null, previousLoader);
            ServerUtilitiesConfig.backups.backup_entire_regions_with_claims = false;
            setCurrentServer(null);
            FileUtils.delete(source);
        }
    }

    @Test
    public void nextBackupRestoresSavingBeforePreparingAgain() throws Exception {
        WorldServer world = mock(WorldServer.class);
        AtomicInteger saves = new AtomicInteger();
        doAnswer(invocation -> {
            if (!world.levelSaving) saves.incrementAndGet();
            return null;
        }).when(world).saveAllChunks(true, null);

        MinecraftServer server = mock(MinecraftServer.class);
        ServerConfigurationManager players = mock(ServerConfigurationManager.class);
        when(server.getConfigurationManager()).thenReturn(players);
        server.worldServers = new WorldServer[] { world };

        File source = new File("build/test-backup-source");
        assertTrue(source.mkdirs() || source.isDirectory());
        ISaveFormat saveFormat = mock(ISaveFormat.class);
        SaveHandler saveHandler = mock(SaveHandler.class);
        when(server.getActiveAnvilConverter()).thenReturn(saveFormat);
        when(saveFormat.getSaveLoader(server.getFolderName(), false)).thenReturn(saveHandler);
        when(saveHandler.getWorldDirectory()).thenReturn(source);
        setCurrentServer(server);

        try {
            Universe universe = new Universe(server);
            new BackupTask(mock(ICommandSender.class), "first").execute(universe);
            waitForBackup();
            new BackupTask(mock(ICommandSender.class), "second").execute(universe);
            waitForBackup();
            new BackupTask(true).execute(universe);
        } finally {
            setCurrentServer(null);
            FileUtils.delete(source);
        }

        assertFalse(world.levelSaving);
        assertTrue("Both backups must save the world", saves.get() == 2);
    }

    @Test
    public void preparationFailureRestoresAlreadyDisabledWorlds() throws Exception {
        WorldServer first = mock(WorldServer.class);
        WorldServer failing = mock(WorldServer.class);
        doThrow(new MinecraftException("injected failure")).when(failing).saveAllChunks(true, null);

        try {
            BackupTask.saveAndDisableWorldSaving(new WorldServer[] { first, failing });
        } catch (MinecraftException expected) {
            assertFalse(first.levelSaving);
            assertFalse(failing.levelSaving);
            return;
        }
        throw new AssertionError("Expected preparation to fail");
    }

    @Test
    public void restoresCapturedWorldsByIdentity() throws Exception {
        WorldServer first = mock(WorldServer.class);
        WorldServer second = mock(WorldServer.class);
        WorldServer loadedLater = mock(WorldServer.class);
        second.levelSaving = true;

        BackupTask.saveAndDisableWorldSaving(new WorldServer[] { first, second });
        BackupTask.restoreWorldSaving();

        assertFalse(first.levelSaving);
        assertTrue(second.levelSaving);
        assertFalse(loadedLater.levelSaving);
    }

    @Test
    public void stoppingWaitsForWorkerBeforeRestoringSaving() throws Exception {
        WorldServer world = mock(WorldServer.class);
        BackupTask.saveAndDisableWorldSaving(new WorldServer[] { world });
        CountDownLatch started = new CountDownLatch(1);
        ThreadBackup worker = new ThreadBackup(null, null, "", Collections.emptySet()) {

            @Override
            public void run() {
                started.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException ignored) {}
            }
        };
        BackupTask.thread = worker;
        worker.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));

        BackupTask.stopBackupThread();

        assertFalse(worker.isAlive());
        assertFalse(world.levelSaving);
        assertNull(BackupTask.thread);
    }

    @Test
    public void asynchronousBackupUsesPreparedPlayerSnapshot() throws Exception {
        File source = new File("build/test-snapshot-world");
        File player = new File(source, "playerdata/player.dat");
        assertTrue(player.getParentFile().mkdirs() || player.getParentFile().isDirectory());
        Files.write(player.toPath(), "before".getBytes(StandardCharsets.UTF_8));

        String entryName = FileUtils.getRelativePath(player);
        Map<String, File> snapshot = ThreadBackup.snapshotFiles(source);
        Files.write(player.toPath(), "after".getBytes(StandardCharsets.UTF_8));
        ThreadBackup.doBackup(ICompress.createCompressor(), source, "snapshot-test", Collections.emptySet(), snapshot);

        try (ZipFile zip = new ZipFile(new File(BackupTask.BACKUP_FOLDER, "snapshot-test.zip"))) {
            ZipEntry entry = zip.getEntry(entryName);
            assertTrue(entry != null);
            try (InputStream in = zip.getInputStream(entry)) {
                assertEquals("before", new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8));
            }
        } finally {
            FileUtils.delete(source);
            ThreadBackup.deleteSnapshot();
        }
    }

    private static void waitForBackup() throws InterruptedException {
        ThreadBackup worker = BackupTask.thread;
        worker.join(TimeUnit.SECONDS.toMillis(5));
        assertFalse("Backup worker did not stop", worker.isAlive());
    }

    private static void setCurrentServer(MinecraftServer server) throws ReflectiveOperationException {
        Field field = MinecraftServer.class.getDeclaredField("mcServer");
        field.setAccessible(true);
        field.set(null, server);
    }
}
