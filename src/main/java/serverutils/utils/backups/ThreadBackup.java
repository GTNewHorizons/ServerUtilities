package serverutils.utils.backups;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.lib.lib.util.BackupUtils;
import serverutils.lib.lib.util.BroadcastSender;
import serverutils.lib.lib.util.StringUtils;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesConfig;

public class ThreadBackup extends Thread {

    private File src0;
    public boolean isDone = false;

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
        BackupUtils.Time time = BackupUtils.Time.now();
        File dstFile = null;

        try {
            StringBuilder out = new StringBuilder();
            appendNum(out, time.year, '-');
            appendNum(out, time.month, '-');
            appendNum(out, time.day, '-');
            appendNum(out, time.hours, '-');
            appendNum(out, time.minutes, '-');
            appendNum(out, time.seconds, File.separatorChar);

            List<File> files = BackupUtils.listAll(src);
            int allFiles = files.size();

            Backups.logger.info("Backing up " + files.size() + " files...");

            if (ServerUtilitiesConfig.backups.compression_level > 0) {
                out.append("backup.zip");
                dstFile = BackupUtils.newFile(new File(Backups.backupsFolder, out.toString()));

                long start = BackupUtils.millis();

                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dstFile));
                // zos.setLevel(9);
                zos.setLevel(ServerUtilitiesConfig.backups.compression_level);

                long logMillis = BackupUtils.millis() + 5000L;

                byte[] buffer = new byte[4096];

                Backups.logger.info("Compressing " + allFiles + " files!");

                for (int i = 0; i < allFiles; i++) {
                    File file = files.get(i);
                    String filePath = file.getAbsolutePath();
                    ZipEntry ze = new ZipEntry(
                            src.getName() + File.separator
                                    + filePath.substring(src.getAbsolutePath().length() + 1, filePath.length()));

                    long millis = BackupUtils.millis();

                    if (i == 0 || millis > logMillis || i == allFiles - 1) {
                        logMillis = millis + 5000L;

                        StringBuilder log = new StringBuilder();
                        log.append('[');
                        log.append(i);
                        log.append(" | ");
                        log.append(BackupUtils.toSmallDouble((i / (double) allFiles) * 100D));
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
                                + BackupUtils.getSizeS(dstFile)
                                + ")!");
            } else {
                out.append(src.getName());
                dstFile = new File(Backups.backupsFolder, out.toString());
                dstFile.mkdirs();

                String dstPath = dstFile.getAbsolutePath() + File.separator;
                String srcPath = src.getAbsolutePath();

                long logMillis = BackupUtils.millis() + 2000L;

                for (int i = 0; i < allFiles; i++) {
                    File file = files.get(i);

                    long millis = BackupUtils.millis();

                    if (i == 0 || millis > logMillis || i == allFiles - 1) {
                        logMillis = millis + 2000L;

                        StringBuilder log = new StringBuilder();
                        log.append('[');
                        log.append(i);
                        log.append(" | ");
                        log.append(BackupUtils.toSmallDouble((i / (double) allFiles) * 100D));
                        log.append("%]: ");
                        log.append(file.getName());
                        Backups.logger.info(log.toString());
                    }

                    File dst1 = new File(dstPath + (file.getAbsolutePath().replace(srcPath, "")));
                    // LMUtils.throwException(BackupUtils.copyFile(file, dst1));
                }
            }

            Backups.logger.info("Created " + dstFile.getAbsolutePath() + " from " + src.getAbsolutePath());

            Backups.clearOldBackups();

            if (ServerUtilitiesConfig.backups.display_file_size) {
                String sizeB = BackupUtils.getSizeS(dstFile);
                String sizeT = BackupUtils.getSizeS(Backups.backupsFolder);

                IChatComponent c = ServerUtilities.lang(
                        null,
                        "cmd.backup_end_2",
                        getDoneTime(time.millis),
                        (sizeB.equals(sizeT) ? sizeB : (sizeB + " | " + sizeT)));
                c.getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE);
                BroadcastSender.inst.addChatMessage(c);
            } else {
                IChatComponent c = ServerUtilities.lang(null, "cmd.backup_end_1", getDoneTime(time.millis));
                c.getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE);
                BroadcastSender.inst.addChatMessage(c);
            }
        } catch (Exception e) {
            IChatComponent c = ServerUtilities
                    .lang(null, "cmd.backup_fail", e.getClass() == null ? null : e.getClass().getName());
            c.getChatStyle().setColor(EnumChatFormatting.DARK_RED);
            BroadcastSender.inst.addChatMessage(c);

            e.printStackTrace();
            if (dstFile != null) BackupUtils.delete(dstFile);
        }
    }

    private static String getDoneTime(long l) {
        return StringUtils.getTimeString(BackupUtils.millis() - l);
    }

    private static void appendNum(StringBuilder sb, int num, char c) {
        if (num < 10) sb.append('0');
        sb.append(num);
        if (c != 0) sb.append(c);
    }
}
