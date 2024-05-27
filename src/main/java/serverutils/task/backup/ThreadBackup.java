package serverutils.task.backup;

import static serverutils.ServerUtilitiesNotifications.BACKUP_END1;
import static serverutils.ServerUtilitiesNotifications.BACKUP_END2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesNotifications;
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

        if (customName.isEmpty()) {
            out.append(time);
        } else {
            out.append(customName);
        }

        try {
            List<File> files = FileUtils.listTree(src);
            int allFiles = files.size();

            ServerUtilities.LOGGER.info("Backing up {} files...", files.size());
            long start = System.currentTimeMillis();
            if (ServerUtilitiesConfig.backups.compression_level > 0) {
                out.append(".zip");
                dstFile = FileUtils.newFile(new File(BackupTask.backupsFolder, out.toString()));
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dstFile));
                zos.setLevel(ServerUtilitiesConfig.backups.compression_level);

                long logMillis = System.currentTimeMillis() + 5000L;

                byte[] buffer = new byte[4096];

                ServerUtilities.LOGGER.info("Compressing {} files!", allFiles);

                for (int i = 0; i < allFiles; i++) {
                    File file = files.get(i);
                    String filePath = file.getAbsolutePath();
                    ZipEntry ze = new ZipEntry(
                            src.getName() + File.separator + filePath.substring(src.getAbsolutePath().length() + 1));

                    long millis = System.currentTimeMillis();

                    if (i == 0 || millis > logMillis || i == allFiles - 1) {
                        logMillis = millis + 5000L;

                        StringBuilder log = new StringBuilder();
                        log.append('[');
                        log.append(i);
                        log.append(" | ");
                        log.append(StringUtils.formatDouble00((i / (double) allFiles) * 100D));
                        log.append("%]: ");
                        log.append(ze.getName());
                        ServerUtilities.LOGGER.info(log.toString());
                    }

                    zos.putNextEntry(ze);
                    FileInputStream fis = new FileInputStream(file);

                    int len;
                    while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);
                    zos.closeEntry();
                    fis.close();
                }

                zos.close();

                ServerUtilities.LOGGER.info(
                        "Done compressing in " + getDoneTime(start)
                                + " seconds ("
                                + FileUtils.getSizeString(dstFile)
                                + ")!");
            } else {
                out.append(File.separatorChar).append(src.getName());
                dstFile = new File(BackupTask.backupsFolder, out.toString());
                dstFile.mkdirs();

                String dstPath = dstFile.getAbsolutePath() + File.separator;
                String srcPath = src.getAbsolutePath();

                long logMillis = System.currentTimeMillis() + 2000L;

                for (int i = 0; i < allFiles; i++) {
                    File file = files.get(i);

                    long millis = System.currentTimeMillis();

                    if (i == 0 || millis > logMillis || i == allFiles - 1) {
                        logMillis = millis + 2000L;

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

            if (ServerUtilitiesConfig.backups.display_file_size) {
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

            e.printStackTrace();
            if (dstFile != null) FileUtils.delete(dstFile);
        }
    }

    private static String getDoneTime(long l) {
        return StringUtils.getTimeString(System.currentTimeMillis() - l);
    }
}
