package serverutils.utils.mod;

import net.minecraft.util.*;

import latmod.lib.*;
import serverutils.lib.*;
import serverutils.utils.badges.ServerBadges;
import serverutils.utils.mod.cmd.admin.CmdRestart;
import serverutils.utils.mod.config.*;
import serverutils.utils.mod.handlers.ServerUtilitiesChunkEventHandler;
import serverutils.utils.world.Backups;

public class ServerUtilsTicks {

    public static long nextChunkloaderUpdate = 0L;

    private static long startMillis = 0L;
    private static String lastRestartMessage = "";
    public static long restartMillis = 0L;

    public static void serverStarted() {
        startMillis = LMUtils.millis();
        Backups.nextBackup = startMillis + ServerUtilitiesConfigBackups.backupMillis();

        if (ServerUtilitiesConfigGeneral.restart_timer.getAsDouble() > 0D) {
            restartMillis = startMillis
                    + (long) (ServerUtilitiesConfigGeneral.restart_timer.getAsDouble() * 3600D * 1000D);
            ServerUtilities.logger.info("Server restart in " + LMStringUtils.getTimeString(restartMillis));
        }

        nextChunkloaderUpdate = startMillis + 10000L;
    }

    public static void serverStopped() {
        startMillis = restartMillis = 0L;
    }

    public static void update() {
        long now = LMUtils.millis();

        if (restartMillis > 0L) {
            int secondsLeft = (int) ((restartMillis - LMUtils.millis()) / 1000L);

            if (secondsLeft <= 0) {
                CmdRestart.restart();
                return;
            } else {
                String msg = LMStringUtils.getTimeString(secondsLeft * 1000L);
                if (msg != null && !lastRestartMessage.equals(msg)) {
                    lastRestartMessage = msg;

                    if (secondsLeft <= 10 || secondsLeft == 30
                            || secondsLeft == 60
                            || secondsLeft == 300
                            || secondsLeft == 600
                            || secondsLeft == 1800) {
                        IChatComponent c = ServerUtilities.mod.chatComponent("server_restart", msg);
                        c.getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE);
                        ServerUtilitiesLib.printChat(BroadcastSender.inst, c);
                    }
                }
            }
        }

        if (Backups.nextBackup > 0L && Backups.nextBackup <= now) {
            Backups.run(ServerUtilitiesLib.getServer());
        }

        if (nextChunkloaderUpdate < now) {
            nextChunkloaderUpdate = now + 30000L;
            ServerUtilitiesChunkEventHandler.instance.markDirty(null);
        }

        if (Backups.thread != null && Backups.thread.isDone) {
            Backups.thread = null;
            Backups.postBackup();
        }

        if (ServerBadges.thread != null && ServerBadges.thread.isDone) {
            ServerBadges.thread = null;
            ServerBadges.sendToPlayer(null);
        }
    }
}
