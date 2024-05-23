package serverutils.task.backup;

import static serverutils.ServerUtilitiesNotifications.BACKUP_START;

import java.io.File;
import java.util.Arrays;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesNotifications;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.ServerUtils;
import serverutils.task.ITask;

public class BackupTask implements ITask {

    public static File backupsFolder;
    public static ThreadBackup thread;
    public static boolean hadPlayer = false;
    public long nextBackup;
    private final long interval;
    private ICommandSender sender;
    private String customName = "";
    private boolean post = false;

    static {
        backupsFolder = ServerUtilitiesConfig.backups.backup_folder_path.isEmpty() ? new File("/backups/")
                : new File(ServerUtilitiesConfig.backups.backup_folder_path);
        if (!backupsFolder.exists()) backupsFolder.mkdirs();
        clearOldBackups();
        ServerUtilities.LOGGER.info("Backups folder - {}", backupsFolder.getAbsolutePath());
    }

    public BackupTask(double interval) {
        this.interval = Ticks.HOUR.x(interval).millis();
        this.nextBackup = System.currentTimeMillis() + this.interval;
    }

    public BackupTask(@Nullable ICommandSender ics, String customName) {
        this(-1);
        this.customName = customName;
        this.sender = ics;
    }

    public BackupTask(boolean postCleanup) {
        this(0);
        this.post = postCleanup;
    }

    @Override
    public boolean isRepeatable() {
        return !post;
    }

    @Override
    public long getNextTime() {
        return nextBackup;
    }

    @Override
    public long getInterval() {
        return interval;
    }

    @Override
    public void setNextTime(long time) {
        nextBackup = time;
    }

    @Override
    public void execute(Universe universe) {
        if (post) {
            postBackup();
            return;
        }
        if (thread != null) return;
        boolean auto = sender == null;

        if (auto && !ServerUtilitiesConfig.backups.enable_backups) return;

        MinecraftServer server = universe.server;
        nextBackup = System.currentTimeMillis() + interval;

        if (auto && ServerUtilitiesConfig.backups.need_online_players) {
            if (!hasOnlinePlayers(universe) && !hadPlayer) return;
            hadPlayer = false;
        }
        ServerUtilitiesNotifications.backupNotification(BACKUP_START, "cmd.backup_start");

        try {
            for (int i = 0; i < server.worldServers.length; ++i) {
                if (server.worldServers[i] != null) {
                    WorldServer worldserver = server.worldServers[i];
                    worldserver.levelSaving = true;
                    worldserver.saveAllChunks(true, null);
                }
            }
        } catch (Exception ex) {
            ServerUtilities.LOGGER.info("Error while saving world: {}", ex.getMessage());
        }

        File wd = server.getEntityWorld().getSaveHandler().getWorldDirectory();

        if (ServerUtilitiesConfig.backups.use_separate_thread) {
            thread = new ThreadBackup(wd, customName);
            thread.start();
        } else {
            ThreadBackup.doBackup(wd, customName);
        }
        universe.scheduleTask(new BackupTask(true));
    }

    public static void clearOldBackups() {
        String[] backups = backupsFolder.list();

        if (backups != null && backups.length > ServerUtilitiesConfig.backups.backups_to_keep) {
            Arrays.sort(backups);

            int toDelete = backups.length - ServerUtilitiesConfig.backups.backups_to_keep;
            ServerUtilities.LOGGER.info("Deleting {} old backups", toDelete);

            for (int i = 0; i < toDelete; i++) {
                File f = new File(backupsFolder, backups[i]);
                ServerUtilities.LOGGER.info("Deleted old backup: {}", f.getPath());
                FileUtils.delete(f);
            }
        }
    }

    private boolean hasOnlinePlayers(Universe universe) {
        return !universe.server.getConfigurationManager().playerEntityList.isEmpty();
    }

    public static void postBackup() {
        if (thread != null && !thread.isDone) {
            Universe.get().scheduleTask(new BackupTask(true));
            return;
        }

        thread = null;
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
