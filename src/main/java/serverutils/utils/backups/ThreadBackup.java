package serverutils.utils.backups;

import static serverutils.mod.ServerUtilitiesNotifications.BACKUP_END1_ID;
import static serverutils.mod.ServerUtilitiesNotifications.BACKUP_END2_ID;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.lib.lib.util.FileUtils;
import serverutils.lib.lib.util.ServerUtils;
import serverutils.lib.lib.util.StringUtils;
import serverutils.mod.ServerUtilities;
import serverutils.mod.ServerUtilitiesConfig;
import serverutils.mod.ServerUtilitiesNotifications;

public class ThreadBackup extends Thread {

    private File src0;
    public boolean isDone = false;

    public static final DecimalFormat smallDoubleFormatter = new DecimalFormat("#0.00");

    public static String toSmallDouble(double d) {
        return smallDoubleFormatter.format(d);
    }

    public ThreadBackup(File w) {
        src0 = w;
        setPriority(7);
    }

    public void run() {
        isDone = false;
        doBackup(src0);
        isDone = true;
    }

    public static void doBackup(File src) {
        String time = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().getTime());
        File dstFile = null;

        try {
            StringBuilder out = new StringBuilder(time);
            List<File> files = FileUtils.listTree(src);
            int allFiles = files.size();

            Backups.logger.info("Backing up " + files.size() + " files...");

            if (ServerUtilitiesConfig.backups.compression_level > 0) {
                out.append(File.separatorChar).append("backup.zip");
                dstFile = FileUtils.newFile(new File(Backups.backupsFolder, out.toString()));

                long start = System.currentTimeMillis();

                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dstFile));
                // zos.setLevel(9);
                zos.setLevel(ServerUtilitiesConfig.backups.compression_level);

                long logMillis = System.currentTimeMillis() + 5000L;

                byte[] buffer = new byte[4096];

                Backups.logger.info("Compressing " + allFiles + " files!");

                for (int i = 0; i < allFiles; i++) {
                    File file = files.get(i);
                    String filePath = file.getAbsolutePath();
                    ZipEntry ze = new ZipEntry(
                            src.getName() + File.separator
                                    + filePath.substring(src.getAbsolutePath().length() + 1, filePath.length()));

                    long millis = System.currentTimeMillis();

                    if (i == 0 || millis > logMillis || i == allFiles - 1) {
                        logMillis = millis + 5000L;

                        StringBuilder log = new StringBuilder();
                        log.append('[');
                        log.append(i);
                        log.append(" | ");
                        log.append(toSmallDouble((i / (double) allFiles) * 100D));
                        log.append("%]: ");
                        log.append(ze.getName());
                        Backups.logger.info(log.toString());
                    }

                    zos.putNextEntry(ze);
                    FileInputStream fis = new FileInputStream(file);

                    int len;
                    while ((len = fis.read(buffer)) > 0) zos.write(buffer, 0, len);
                    zos.closeEntry();
                    fis.close();
                }

                zos.close();

                Backups.logger.info(
                        "Done compressing in " + getDoneTime(start)
                                + " seconds ("
                                + FileUtils.getSizeString(dstFile)
                                + ")!");
            } else {
                out.append(File.separatorChar).append(src.getName());
                dstFile = new File(Backups.backupsFolder, out.toString());
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
                        log.append(toSmallDouble((i / (double) allFiles) * 100D));
                        log.append("%]: ");
                        log.append(file.getName());
                        Backups.logger.info(log.toString());
                    }

                    File dst1 = new File(dstPath + (file.getAbsolutePath().replace(srcPath, "")));
                    FileUtils.copyFile(file, dst1);
                }
            }

            Backups.logger.info("Created " + dstFile.getAbsolutePath() + " from " + src.getAbsolutePath());

            Backups.clearOldBackups();

            if (ServerUtilitiesConfig.backups.display_file_size) {
                String sizeB = FileUtils.getSizeString(dstFile);
                String sizeT = FileUtils.getSizeString(Backups.backupsFolder);
                ServerUtilitiesNotifications.backupNotification(
                        BACKUP_END2_ID,
                        "cmd.backup_end_2",
                        getDoneTime(Calendar.getInstance().getTimeInMillis()),
                        (sizeB.equals(sizeT) ? sizeB : (sizeB + " | " + sizeT)));
            } else {
                ServerUtilitiesNotifications.backupNotification(
                        BACKUP_END1_ID,
                        "cmd.backup_end_1",
                        getDoneTime(Calendar.getInstance().getTimeInMillis()));
            }
        } catch (Exception e) {
            IChatComponent c = StringUtils.color(
                    ServerUtilities.lang(null, "cmd.backup_fail", e.getClass() == null ? null : e.getClass().getName()),
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
