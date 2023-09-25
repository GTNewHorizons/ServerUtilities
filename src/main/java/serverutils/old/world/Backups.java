package serverutils.old.world;

import java.io.File;
import java.util.Arrays;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandSaveAll;
import net.minecraft.command.server.CommandSaveOff;
import net.minecraft.command.server.CommandSaveOn;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import latmod.lib.LMFileUtils;
import latmod.lib.LMUtils;
import serverutils.lib.BroadcastSender;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.old.mod.ServerUtilities;
import serverutils.old.mod.config.ServerUtilitiesConfigBackups;

public class Backups {

    public static final Logger logger = LogManager.getLogger("ServerUtilities Backups");

    public static File backupsFolder;
    public static long nextBackup = -1L;
    public static ThreadBackup thread = null;
    public static boolean hadPlayer = false;

    public static void init() {
        backupsFolder = ServerUtilitiesConfigBackups.folder.getAsString().isEmpty()
                ? new File(ServerUtilitiesLib.folderMinecraft, "/backups/")
                : new File(ServerUtilitiesConfigBackups.folder.getAsString());
        if (!backupsFolder.exists()) backupsFolder.mkdirs();
        thread = null;
        clearOldBackups();
        logger.info("Backups folder - " + backupsFolder.getAbsolutePath());
    }

    public static boolean run(ICommandSender ics) {
        if (thread != null) return false;
        boolean auto = !(ics instanceof EntityPlayerMP);

        if (auto && !ServerUtilitiesConfigBackups.enabled.getAsBoolean()) return false;

        World w = ServerUtilitiesLib.getServerWorld();
        if (w == null) return false;

        nextBackup = LMUtils.millis() + ServerUtilitiesConfigBackups.backupMillis();

        if (auto && ServerUtilitiesConfigBackups.need_online_players.getAsBoolean()) {
            if (!ServerUtilitiesLib.hasOnlinePlayers() && !hadPlayer) return true;
            hadPlayer = false;
        }

        IChatComponent c = ServerUtilities.mod.chatComponent("cmd.backup_start", ics.getCommandSenderName());
        c.getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE);
        BroadcastSender.inst.addChatMessage(c);

        try {
            new CommandSaveOff().processCommand(ServerUtilitiesLib.getServer(), new String[0]);
            new CommandSaveAll().processCommand(ServerUtilitiesLib.getServer(), new String[0]);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        File wd = w.getSaveHandler().getWorldDirectory();

        if (ServerUtilitiesConfigBackups.use_separate_thread.getAsBoolean()) {
            thread = new ThreadBackup(wd);
            thread.start();
        } else {
            ThreadBackup.doBackup(wd);
        }

        return true;
    }

    public static void clearOldBackups() {
        String[] s = backupsFolder.list();

        if (s.length > ServerUtilitiesConfigBackups.backups_to_keep.getAsInt()) {
            Arrays.sort(s);

            int j = s.length - ServerUtilitiesConfigBackups.backups_to_keep.getAsInt();
            logger.info("Deleting " + j + " old backups");

            for (int i = 0; i < j; i++) {
                File f = new File(backupsFolder, s[i]);
                if (f.isDirectory()) {
                    logger.info("Deleted old backup: " + f.getPath());
                    LMFileUtils.delete(f);
                }
            }
        }
    }

    public static void postBackup() {
        try {
            new CommandSaveOn().processCommand(ServerUtilitiesLib.getServer(), new String[0]);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
