package serverutils.backups;

import static serverutils.ServerUtilitiesNotifications.BACKUP_START;

import java.io.File;
import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesNotifications;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.ServerUtils;

public class Backups {

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
        ServerUtilities.LOGGER.info("Backups folder - " + backupsFolder.getAbsolutePath());
    }

    public static boolean run(ICommandSender ics, String customName) {
        if (thread != null) return false;
        boolean auto = !(ics instanceof EntityPlayerMP);

        if (auto && !ServerUtilitiesConfig.backups.enable_backups) return false;

        MinecraftServer server = ServerUtils.getServer();
        nextBackup = System.currentTimeMillis() + backupMillis();

        if (auto && ServerUtilitiesConfig.backups.need_online_players) {
            if (!hasOnlinePlayers() && !hadPlayer) return true;
            hadPlayer = false;
        }
        ServerUtilitiesNotifications.backupNotification(BACKUP_START, "cmd.backup_start", ics.getCommandSenderName());

        try {
            for (int i = 0; i < server.worldServers.length; ++i) {
                if (server.worldServers[i] != null) {
                    WorldServer worldserver = server.worldServers[i];
                    worldserver.levelSaving = true;
                    worldserver.saveAllChunks(true, null);
                }
            }
        } catch (Exception ex) {
            ServerUtilities.LOGGER.info("Error while saving world: " + ex.getMessage());
        }

        File wd = server.getEntityWorld().getSaveHandler().getWorldDirectory();

        if (ServerUtilitiesConfig.backups.use_separate_thread) {
            thread = new ThreadBackup(wd, customName);
            thread.start();
        } else {
            ThreadBackup.doBackup(wd, customName);
        }

        return true;
    }

    public static void clearOldBackups() {
        String[] s = backupsFolder.list();

        if (s.length > ServerUtilitiesConfig.backups.backups_to_keep) {
            Arrays.sort(s);

            int j = s.length - ServerUtilitiesConfig.backups.backups_to_keep;
            ServerUtilities.LOGGER.info("Deleting " + j + " old backups");

            for (int i = 0; i < j; i++) {
                File f = new File(backupsFolder, s[i]);
                if (f.isDirectory()) {
                    ServerUtilities.LOGGER.info("Deleted old backup: " + f.getPath());
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
            MinecraftServer server = ServerUtils.getServer();

            for (int i = 0; i < server.worldServers.length; ++i) {
                if (server.worldServers[i] != null) {
                    WorldServer worldserver = server.worldServers[i];

                    if (worldserver.levelSaving) {
                        worldserver.levelSaving = false;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
