package serverutils.task.backup;

import static serverutils.ServerUtilitiesConfig.backups;
import static serverutils.ServerUtilitiesNotifications.BACKUP_END1;
import static serverutils.ServerUtilitiesNotifications.BACKUP_END2;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesNotifications;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.StringUtils;

public class ThreadBackup extends Thread {

    private final File src0;
    private final String customName;
    public boolean isDone = false;

    public ThreadBackup(File w, String s) {
        src0 = w;
        customName = s;
        setPriority(7);
    }

    public void run() {
        isDone = false;
        doBackup(src0, customName);
        isDone = true;
    }

    public static void doBackup(File src, String customName) {

        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().getTime());
        File dstFile = null;
        StringBuilder out = new StringBuilder();
        out.append(customName.isEmpty() ? time : customName);

        try {
            List<File> files = FileUtils.listTree(src);
            int allFiles = files.size();

            ServerUtilities.LOGGER.info("Backing up {} files...", files.size());
            long start = System.currentTimeMillis();
            if (backups.compression_level > 0) {
                out.append(".zip");
                dstFile = FileUtils.newFile(new File(BackupTask.backupsFolder, out.toString()));
                try (ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(dstFile)) {
                    zaos.setLevel(backups.compression_level);

                    long logMillis = System.currentTimeMillis() + Ticks.SECOND.x(5).millis();
                    ServerUtilities.LOGGER.info("Compressing {} files!", allFiles);

                    for (int i = 0; i < allFiles; i++) {
                        File file = files.get(i);
                        String filePath = file.getAbsolutePath();
                        ZipArchiveEntry ze = new ZipArchiveEntry(
                                src.getName() + File.separator
                                        + filePath.substring(src.getAbsolutePath().length() + 1));

                        long millis = System.currentTimeMillis();

                        if (i == 0 || millis > logMillis || i == allFiles - 1) {
                            logMillis = millis + Ticks.SECOND.x(5).millis();

                            StringBuilder log = new StringBuilder();
                            log.append('[');
                            log.append(i);
                            log.append(" | ");
                            log.append(StringUtils.formatDouble00((i / (double) allFiles) * 100D));
                            log.append("%]: ");
                            log.append(ze.getName());
                            ServerUtilities.LOGGER.info(log.toString());
                        }

                        zaos.putArchiveEntry(ze);

                        try (FileInputStream fis = new FileInputStream(file)) {
                            IOUtils.copy(fis, zaos);
                        }
                        zaos.closeArchiveEntry();
                    }
                }
                ServerUtilities.LOGGER.info(
                        "Done compressing in {} seconds ({})!",
                        getDoneTime(start),
                        FileUtils.getSizeString(dstFile));
            } else {
                out.append(File.separatorChar).append(src.getName());
                dstFile = FileUtils.newFile(new File(BackupTask.backupsFolder, out.toString()));;

                String dstPath = dstFile.getAbsolutePath() + File.separator;
                String srcPath = src.getAbsolutePath();

                long logMillis = System.currentTimeMillis() + Ticks.SECOND.x(2).millis();

                for (int i = 0; i < allFiles; i++) {
                    File file = files.get(i);

                    long millis = System.currentTimeMillis();

                    if (i == 0 || millis > logMillis || i == allFiles - 1) {
                        logMillis = millis + Ticks.SECOND.x(2).millis();

                        StringBuilder log = new StringBuilder();
                        log.append('[');
                        log.append(i);
                        log.append(" | ");
                        log.append(StringUtils.formatDouble00((i / (double) allFiles) * 100D));
                        log.append("%]: ");
                        log.append(file.getName());
                        ServerUtilities.LOGGER.info(log.toString());
                    }

                    File dst1 = new File(dstPath + (file.getAbsolutePath().replace(srcPath, "")));
                    FileUtils.copyFile(file, dst1);
                }
            }

            ServerUtilities.LOGGER.info("Created {} from {}", dstFile.getAbsolutePath(), src.getAbsolutePath());

            BackupTask.clearOldBackups();

            if (backups.display_file_size) {
                String sizeB = FileUtils.getSizeString(dstFile);
                String sizeT = FileUtils.getSizeString(BackupTask.backupsFolder);
                ServerUtilitiesNotifications.backupNotification(
                        BACKUP_END2,
                        "cmd.backup_end_2",
                        getDoneTime(start),
                        (sizeB.equals(sizeT) ? sizeB : (sizeB + " | " + sizeT)));
            } else {
                ServerUtilitiesNotifications.backupNotification(BACKUP_END1, "cmd.backup_end_1", getDoneTime(start));
            }
        } catch (Exception e) {
            IChatComponent c = StringUtils.color(
                    ServerUtilities.lang(null, "cmd.backup_fail", e.getClass().getName()),
                    EnumChatFormatting.RED);
            ServerUtils.notifyChat(ServerUtils.getServer(), null, c);
            ServerUtilities.LOGGER.error("Error while backing up: {}", e.getMessage());

            if (dstFile != null) FileUtils.delete(dstFile);
        }
    }

    private static String getDoneTime(long l) {
        return StringUtils.getTimeString(System.currentTimeMillis() - l);
    }
}
