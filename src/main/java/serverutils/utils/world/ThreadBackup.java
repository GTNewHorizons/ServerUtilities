package serverutils.utils.world;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import latmod.lib.LMFileUtils;
import latmod.lib.LMStringUtils;
import latmod.lib.LMUtils;
import latmod.lib.MathHelperLM;
import latmod.lib.Time;
import serverutils.lib.BroadcastSender;
import serverutils.utils.mod.ServerUtilities;
import serverutils.utils.mod.config.ServerUtilitiesConfigBackups;

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
        Time time = Time.now();
        File dstFile = null;

        try {
            StringBuilder out = new StringBuilder();
            appendNum(out, time.year, '-');
            appendNum(out, time.month, '-');
            appendNum(out, time.day, '-');
            appendNum(out, time.hours, '-');
            appendNum(out, time.minutes, '-');
            appendNum(out, time.seconds, File.separatorChar);

            List<File> files = LMFileUtils.listAll(src);
            int allFiles = files.size();

            Backups.logger.info("Backing up " + files.size() + " files...");

            if (ServerUtilitiesConfigBackups.compression_level.getAsInt() > 0) {
                out.append("backup.zip");
                dstFile = LMFileUtils.newFile(new File(Backups.backupsFolder, out.toString()));

                long start = LMUtils.millis();

                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dstFile));
                // zos.setLevel(9);
                zos.setLevel(ServerUtilitiesConfigBackups.compression_level.getAsInt());

                long logMillis = LMUtils.millis() + 5000L;

                byte[] buffer = new byte[4096];

                Backups.logger.info("Compressing " + allFiles + " files!");

                for (int i = 0; i < allFiles; i++) {
                    File file = files.get(i);
                    String filePath = file.getAbsolutePath();
                    ZipEntry ze = new ZipEntry(
                            src.getName() + File.separator
                                    + filePath.substring(src.getAbsolutePath().length() + 1, filePath.length()));

                    long millis = LMUtils.millis();

                    if (i == 0 || millis > logMillis || i == allFiles - 1) {
                        logMillis = millis + 5000L;

                        StringBuilder log = new StringBuilder();
                        log.append('[');
                        log.append(i);
                        log.append(" | ");
                        log.append(MathHelperLM.toSmallDouble((i / (double) allFiles) * 100D));
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
                                + LMFileUtils.getSizeS(dstFile)
                                + ")!");
            } else {
                out.append(src.getName());
                dstFile = new File(Backups.backupsFolder, out.toString());
                dstFile.mkdirs();

                String dstPath = dstFile.getAbsolutePath() + File.separator;
                String srcPath = src.getAbsolutePath();

                long logMillis = LMUtils.millis() + 2000L;

                for (int i = 0; i < allFiles; i++) {
                    File file = files.get(i);

                    long millis = LMUtils.millis();

                    if (i == 0 || millis > logMillis || i == allFiles - 1) {
                        logMillis = millis + 2000L;

                        StringBuilder log = new StringBuilder();
                        log.append('[');
                        log.append(i);
                        log.append(" | ");
                        log.append(MathHelperLM.toSmallDouble((i / (double) allFiles) * 100D));
                        log.append("%]: ");
                        log.append(file.getName());
                        Backups.logger.info(log.toString());
                    }

                    File dst1 = new File(dstPath + (file.getAbsolutePath().replace(srcPath, "")));
                    LMUtils.throwException(LMFileUtils.copyFile(file, dst1));
                }
            }

            Backups.logger.info("Created " + dstFile.getAbsolutePath() + " from " + src.getAbsolutePath());

            Backups.clearOldBackups();

            if (ServerUtilitiesConfigBackups.display_file_size.getAsBoolean()) {
                String sizeB = LMFileUtils.getSizeS(dstFile);
                String sizeT = LMFileUtils.getSizeS(Backups.backupsFolder);

                IChatComponent c = ServerUtilities.mod.chatComponent(
                        "cmd.backup_end_2",
                        getDoneTime(time.millis),
                        (sizeB.equals(sizeT) ? sizeB : (sizeB + " | " + sizeT)));
                c.getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE);
                BroadcastSender.inst.addChatMessage(c);
            } else {
                IChatComponent c = ServerUtilities.mod.chatComponent("cmd.backup_end_1", getDoneTime(time.millis));
                c.getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE);
                BroadcastSender.inst.addChatMessage(c);
            }
        } catch (Exception e) {
            IChatComponent c = ServerUtilities.mod.chatComponent("cmd.backup_fail", LMUtils.classpath(e.getClass()));
            c.getChatStyle().setColor(EnumChatFormatting.DARK_RED);
            BroadcastSender.inst.addChatMessage(c);

            e.printStackTrace();
            if (dstFile != null) LMFileUtils.delete(dstFile);
        }
        // System.gc();
    }

    private static String getDoneTime(long l) {
        return LMStringUtils.getTimeString(LMUtils.millis() - l);
    }

    private static void appendNum(StringBuilder sb, int num, char c) {
        if (num < 10) sb.append('0');
        sb.append(num);
        if (c != 0) sb.append(c);
    }
}
