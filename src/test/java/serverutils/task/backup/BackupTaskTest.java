package serverutils.task.backup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.SaveHandler;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.data.Universe;
import serverutils.lib.util.FileUtils;

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
