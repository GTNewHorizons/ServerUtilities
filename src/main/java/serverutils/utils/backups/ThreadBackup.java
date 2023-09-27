package serverutils.utils.backups;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import serverutils.lib.lib.util.FileUtils;
import serverutils.lib.lib.util.ServerUtils;
import serverutils.lib.lib.util.StringUtils;
import serverutils.lib.lib.util.text_components.Notification;
import serverutils.utils.ServerUtilities;
import serverutils.utils.ServerUtilitiesConfig;

public class ThreadBackup extends Thread {

    public static final ResourceLocation BACKUP_END1_ID = new ResourceLocation(ServerUtilities.MOD_ID, "backup_end1");
    public static final ResourceLocation BACKUP_END2_ID = new ResourceLocation(ServerUtilities.MOD_ID, "backup_end2");
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

                long start = System.currentTimeMillis();;

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

                    long millis = System.currentTimeMillis();;

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
                out.append(src.getName());
                dstFile = new File(Backups.backupsFolder, out.toString());
                dstFile.mkdirs();

                String dstPath = dstFile.getAbsolutePath() + File.separator;
                String srcPath = src.getAbsolutePath();

                long logMillis = System.currentTimeMillis() + 2000L;

                for (int i = 0; i < allFiles; i++) {
                    File file = files.get(i);

                    long millis = System.currentTimeMillis();;

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
                    // LMUtils.throwException(BackupUtils.copyFile(file, dst1));
                }
            }

            Backups.logger.info("Created " + dstFile.getAbsolutePath() + " from " + src.getAbsolutePath());

            Backups.clearOldBackups();

            if (ServerUtilitiesConfig.backups.display_file_size) {
                String sizeB = FileUtils.getSizeString(dstFile);
                String sizeT = FileUtils.getSizeString(Backups.backupsFolder);
                for (EntityPlayerMP player : (List<EntityPlayerMP>) ServerUtils.getServer()
                        .getConfigurationManager().playerEntityList) {
                    Notification.of(
                            BACKUP_END2_ID,
                            StringUtils.color(
                                    ServerUtilities.lang(
                                            null,
                                            "cmd.backup_end_2",
                                            getDoneTime(Calendar.getInstance().getTimeInMillis()),
                                            (sizeB.equals(sizeT) ? sizeB : (sizeB + " | " + sizeT))),
                                    EnumChatFormatting.LIGHT_PURPLE))
                            .send(ServerUtils.getServer(), player);
                }
            } else {
                for (EntityPlayerMP player : (List<EntityPlayerMP>) ServerUtils.getServer()
                        .getConfigurationManager().playerEntityList) {
                    Notification.of(
                            BACKUP_END1_ID,
                            StringUtils.color(
                                    ServerUtilities.lang(
                                            null,
                                            "cmd.backup_end_1",
                                            getDoneTime(Calendar.getInstance().getTimeInMillis())),
                                    EnumChatFormatting.LIGHT_PURPLE))
                            .send(ServerUtils.getServer(), player);
                }
            }
        } catch (Exception e) {
            IChatComponent c = ServerUtilities
                    .lang(null, "cmd.backup_fail", e.getClass() == null ? null : e.getClass().getName());
            c.getChatStyle().setColor(EnumChatFormatting.DARK_RED);
            ServerUtils.notify(ServerUtils.getServer(), null, c);

            e.printStackTrace();
            if (dstFile != null) FileUtils.delete(dstFile);
        }
    }

    private static String getDoneTime(long l) {
        return StringUtils.getTimeString(System.currentTimeMillis() - l);
    }

    private static void appendNum(StringBuilder sb, int num, char c) {
        if (num < 10) sb.append('0');
        sb.append(num);
        if (c != 0) sb.append(c);
    }
}