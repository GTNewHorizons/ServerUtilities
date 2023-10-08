package serverutils.utils.backups;

import static serverutils.mod.ServerUtilitiesNotifications.BACKUP_START_ID;

import java.io.File;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandSaveAll;
import net.minecraft.command.server.CommandSaveOff;
import net.minecraft.command.server.CommandSaveOn;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import serverutils.lib.lib.util.FileUtils;
import serverutils.lib.lib.util.ServerUtils;
import serverutils.mod.ServerUtilitiesConfig;
import serverutils.mod.ServerUtilitiesNotifications;

public class Backups {

    public static final Logger logger = LogManager.getLogger("ServerUtilities Backup");
    public static File backupsFolder;
    public static long nextBackup = -1L;
    public static ThreadBackup thread = null;
    public static boolean hadPlayer = false;

    public static void init() {
        backupsFolder = ServerUtilitiesConfig.backups.backup_folder_path.isEmpty()
                ? new File(Minecraft.getMinecraft().mcDataDir, "/backups/")
                : new File(ServerUtilitiesConfig.backups.backup_folder_path);
        if (!backupsFolder.exists()) backupsFolder.mkdirs();
        thread = null;
        clearOldBackups();
        logger.info("Backups folder - " + backupsFolder.getAbsolutePath());
    }

    public static boolean run(ICommandSender ics) {
        if (thread != null) return false;
        boolean auto = !(ics instanceof EntityPlayerMP);

        if (auto && !ServerUtilitiesConfig.backups.enable_backups) return false;

        World w = ServerUtils.getServerWorld();
        if (w == null) return false;

        nextBackup = System.currentTimeMillis() + backupMillis();

        if (auto && ServerUtilitiesConfig.backups.need_online_players) {
            if (!hasOnlinePlayers() && !hadPlayer) return true;
            hadPlayer = false;
        }
        ServerUtilitiesNotifications
                .backupNotification(BACKUP_START_ID, "cmd.backup_start", ics.getCommandSenderName());

        try {
            new CommandSaveOff().processCommand(ServerUtils.getServer(), new String[0]);
            new CommandSaveAll().processCommand(ServerUtils.getServer(), new String[0]);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        File wd = w.getSaveHandler().getWorldDirectory();

        if (ServerUtilitiesConfig.backups.use_separate_thread) {
            thread = new ThreadBackup(wd);
            thread.start();
        } else {
            ThreadBackup.doBackup(wd);
        }

        return true;
    }

    public static void clearOldBackups() {
        String[] s = backupsFolder.list();

        if (s.length > ServerUtilitiesConfig.backups.backups_to_keep) {
            Arrays.sort(s);

            int j = s.length - ServerUtilitiesConfig.backups.backups_to_keep;
            logger.info("Deleting " + j + " old backups");

            for (int i = 0; i < j; i++) {
                File f = new File(backupsFolder, s[i]);
                if (f.isDirectory()) {
                    logger.info("Deleted old backup: " + f.getPath());
                    FileUtils.delete(f);
                }
            }
        }
    }

    public static long backupMillis() {
        return (long) (ServerUtilitiesConfig.backups.backup_timer * 3600D * 1000D);
    }

    public static boolean hasOnlinePlayers() {
        return !ServerUtils.getServer().getConfigurationManager().playerEntityList.isEmpty();
    }

    public static void postBackup() {
        try {
            new CommandSaveOn().processCommand(ServerUtils.getServer(), new String[0]);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
