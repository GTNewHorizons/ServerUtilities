package serverutils.task;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.text_components.Notification;
import serverutils.lib.util.text_components.TaskNotification;

public class CleanupTask implements ITask {

    private static final ResourceLocation CLEANUP_30 = new ResourceLocation(ServerUtilities.MOD_ID, "cleanup_30");
    private static final ResourceLocation CLEANUP_60 = new ResourceLocation(ServerUtilities.MOD_ID, "cleanup_60");
    private long nextTime;
    private final long interval;
    private final List<TaskNotification> notifications = new ArrayList<>();

    public CleanupTask(double interval) {
        this.interval = Ticks.HOUR.x(interval).millis();
        nextTime = System.currentTimeMillis() + this.interval;
        String notificationString = StatCollector.translateToLocal("serverutilities.task.cleanup_entity");
        TaskNotification notification = new TaskNotification(
                Notification.of(CLEANUP_30, new ChatComponentText(String.format(notificationString, 30))))
                        .setNextTime(nextTime - Ticks.SECOND.x(30).millis());
        notifications.add(notification);
        notification = new TaskNotification(
                Notification.of(CLEANUP_60, new ChatComponentText(String.format(notificationString, 60))))
                        .setNextTime(nextTime - Ticks.SECOND.x(60).millis());
        notifications.add(notification);
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public long getNextTime() {
        return nextTime;
    }

    @Override
    public void setNextTime(long time) {
        this.nextTime = time;
    }

    @Override
    public long getInterval() {
        return interval;
    }

    @Override
    public void notify(Universe universe) {
        if (ServerUtilitiesConfig.tasks.cleanup.silent) return;
        for (TaskNotification notification : notifications) {
            long now = System.currentTimeMillis();
            if (now >= notification.getNextTime()) {
                notification.sendToAll(universe.server);
                notification.setNextTime(now + interval);
            }
        }
    }

    @Override
    public void execute(Universe universe) {
        int removed = 0;
        Predicate<Entity> predicate = ServerUtilitiesConfig.tasks.cleanup.predicate;
        for (World world : universe.server.worldServers) {
            for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
                if (predicate.test(entity)) {
                    entity.setDead();
                    removed++;
                }
            }
        }
        Notification.of(
                "removed_entities",
                new ChatComponentText(
                        StatCollector.translateToLocalFormatted("serverutilities.task.cleanup_removed", removed)))
                .sendToAll(universe.server);
    }
}
