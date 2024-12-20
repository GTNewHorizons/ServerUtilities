package serverutils.task;

import static serverutils.ServerUtilitiesNotifications.RESTART_TIMER;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.FileUtils;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.text_components.Notification;

public class ShutdownTask extends Task {

    public static long shutdownTime = 0L;

    public ShutdownTask() {
        super();
        long now = System.currentTimeMillis();
        shutdownTime = 0L;
        Calendar calendar = Calendar.getInstance();
        int currentTime = calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60
                + calendar.get(Calendar.SECOND);
        IntArrayList times = new IntArrayList(ServerUtilitiesConfig.auto_shutdown.times.length);

        for (String s0 : ServerUtilitiesConfig.auto_shutdown.times) {
            try {
                String[] s = s0.split(":", 2);

                int t = Integer.parseInt(s[0]) * 3600 + Integer.parseInt(s[1]) * 60;

                if (t <= currentTime) {
                    t += 24 * 3600;
                }

                times.add(t);
            } catch (Exception ignored) {}
        }

        times.sort(null);

        for (int time : times) {
            if (time > currentTime) {
                shutdownTime = now + (time - currentTime) * Ticks.SECOND.millis();
                break;
            }
        }
    }

    @Override
    public long getNextTime() {
        return shutdownTime;
    }

    @Override
    public void execute(Universe universe) {
        FileUtils.newFile(universe.server.getFile("autostart.stamp"));
        universe.server.initiateShutdown();
    }

    @Override
    public List<NotifyTask> getNotifications() {
        List<NotifyTask> notifications = new ArrayList<>();
        long now = System.currentTimeMillis();
        if (shutdownTime > 0L) {
            ServerUtilities.LOGGER.info("Server will shut down in {}", StringUtils.getTimeString(shutdownTime - now));

            Ticks[] ticks = { Ticks.MINUTE.x(30), Ticks.MINUTE.x(10), Ticks.MINUTE.x(5), Ticks.MINUTE.x(1),
                    Ticks.SECOND.x(10), Ticks.SECOND.x(9), Ticks.SECOND.x(8), Ticks.SECOND.x(7), Ticks.SECOND.x(6),
                    Ticks.SECOND.x(5), Ticks.SECOND.x(4), Ticks.SECOND.x(3), Ticks.SECOND.x(2), Ticks.SECOND.x(1) };

            for (Ticks t : ticks) {
                Notification notification = RESTART_TIMER.createNotification(
                        StringUtils.color(
                                "serverutilities.lang.timer.shutdown",
                                EnumChatFormatting.LIGHT_PURPLE,
                                t.toTimeString()));
                NotifyTask task = new NotifyTask(shutdownTime - t.millis(), notification);
                notifications.add(task);
            }
        }
        return notifications;
    }
}
