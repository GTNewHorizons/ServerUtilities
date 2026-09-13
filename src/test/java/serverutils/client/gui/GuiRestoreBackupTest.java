package serverutils.client.gui;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.compression.ICompress;
import serverutils.task.backup.BackupTask;

public class GuiRestoreBackupTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder(new File("build"));

    @BeforeClass
    public static void configureBackups() {
        ServerUtilitiesConfig.backups.backup_folder_path = "build/test-backups";
    }

    @Test
    public void preparationProtectsArchivesAndRecoveryFiles() throws Exception {
        Files.createDirectories(BackupTask.BACKUP_FOLDER.toPath());
        Files.createDirectories(BackupTask.BACKUP_TEMP_FOLDER.toPath());
        File otherBackup = Files.createTempFile(BackupTask.BACKUP_FOLDER.toPath(), "restore-test-", ".zip").toFile();
        File staged = Files.createTempFile(BackupTask.BACKUP_TEMP_FOLDER.toPath(), "restore-test-", ".tmp").toFile();
        String[] previousPatterns = ServerUtilitiesConfig.backups.additional_backup_files;
        try {
            for (boolean wildcard : new boolean[] { false, true }) {
                File root = temporary.newFolder();
                File previousRoot = new File(root, "recovery");
                File additionalRecovery = new File(previousRoot, "additional");
                File previousWorld = new File(root, "previous-world");
                File recoveryFile = write(new File(additionalRecovery, "existing.dat"), "recovery");
                File stagedRestore = write(new File(previousRoot, ".su-restore-test/new/value"), "staged");
                File oldWorldFile = write(new File(previousWorld, "level.dat"), "old-world");
                File global = write(new File(root, "settings.dat"), "settings");
                File archive = new File(root, "selected.zip");
                File restored = new File(root, "restored.dat");
                archive(archive, "world", FileUtils.getRelativePath(restored));
                byte[] originalArchive = Files.readAllBytes(archive.toPath());
                String suffix = wildcard ? "/**" : "";
                ServerUtilitiesConfig.backups.additional_backup_files = new String[] {
                        FileUtils.getRelativePath(root) + suffix,
                        FileUtils.getRelativePath(BackupTask.BACKUP_FOLDER) + suffix,
                        FileUtils.getRelativePath(BackupTask.BACKUP_TEMP_FOLDER) + suffix };
                Map<File, File> moved = new LinkedHashMap<>();
                Method prepare = GuiRestoreBackup.class.getDeclaredMethod(
                        "renameAdditionalFiles",
                        File.class,
                        boolean.class,
                        Map.class,
                        File.class,
                        File.class);
                prepare.setAccessible(true);
                prepare.invoke(gui("world"), additionalRecovery, true, moved, archive, previousWorld);
                assertEquals(1, moved.size());
                assertTrue(moved.get(global).isFile());
                assertTrue(moved.get(global).toPath().startsWith(additionalRecovery.toPath()));
                assertFalse(global.exists());
                assertArrayEquals(originalArchive, Files.readAllBytes(archive.toPath()));
                assertTrue(otherBackup.isFile());
                assertTrue(staged.isFile());
                assertTrue(recoveryFile.isFile());
                assertTrue(stagedRestore.isFile());
                assertTrue(oldWorldFile.isFile());
                try (ICompress compressor = ICompress.createCompressor()) {
                    compressor.extractArchive(archive, true, false, null, null);
                }
                assertEquals("restored", new String(Files.readAllBytes(restored.toPath()), StandardCharsets.UTF_8));
            }
        } finally {
            ServerUtilitiesConfig.backups.additional_backup_files = previousPatterns;
            Files.deleteIfExists(otherBackup.toPath());
            Files.deleteIfExists(staged.toPath());
        }
    }

    @Test
    public void archiveInsideWorldRemainsReadableAfterWorldIsMoved() throws Exception {
        File saves = new File("saves");
        Files.createDirectories(saves.toPath());
        File world = Files.createTempDirectory(saves.toPath(), "restore-prep-").toFile();
        File preserved = new File(saves, world.getName() + "_old");
        File recoveryRoot = new File("backups_before_restore");
        Files.createDirectories(recoveryRoot.toPath());
        Set<File> previousRecovery = new HashSet<>(Arrays.asList(recoveryRoot.listFiles()));
        String[] previousPatterns = ServerUtilitiesConfig.backups.additional_backup_files;
        try {
            write(new File(world, "level.dat"), "original");
            File archive = new File(world, "backups/selected.zip");
            archive(archive, world.getName(), "saves/" + world.getName() + "/level.dat");
            ServerUtilitiesConfig.backups.additional_backup_files = new String[] {
                    "saves/" + world.getName() + "/backups/**" };
            GuiRestoreBackup gui = gui(world.getName());
            Method restore = GuiRestoreBackup.class.getDeclaredMethod("loadBackup", File.class, boolean.class);
            restore.setAccessible(true);
            restore.invoke(gui, archive, true);
            verify(gui).closeGui();
            assertEquals(
                    "restored",
                    new String(Files.readAllBytes(new File(world, "level.dat").toPath()), StandardCharsets.UTF_8));
            assertEquals(
                    "original",
                    new String(Files.readAllBytes(new File(preserved, "level.dat").toPath()), StandardCharsets.UTF_8));
            assertTrue(new File(preserved, "backups/selected.zip").isFile());
        } finally {
            ServerUtilitiesConfig.backups.additional_backup_files = previousPatterns;
            FileUtils.delete(world);
            FileUtils.delete(preserved);
            for (File file : recoveryRoot.listFiles()) {
                if (!previousRecovery.contains(file)) FileUtils.delete(file);
            }
        }
    }

    private static GuiRestoreBackup gui(String worldName) throws Exception {
        GuiRestoreBackup gui = mock(GuiRestoreBackup.class);
        Field name = GuiRestoreBackup.class.getDeclaredField("worldName");
        name.setAccessible(true);
        name.set(gui, worldName);
        return gui;
    }

    private static File write(File file, String contents) throws Exception {
        Files.createDirectories(file.toPath().getParent());
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static void archive(File file, String worldName, String entry) throws Exception {
        Files.createDirectories(file.toPath().getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
            zip.setComment(worldName);
            zip.putNextEntry(new ZipEntry(entry));
            zip.write("restored".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }
}
