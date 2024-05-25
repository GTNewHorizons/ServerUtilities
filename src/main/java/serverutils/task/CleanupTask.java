package serverutils.task;

import java.util.ArrayList;
import java.util.function.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.INpc;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.text_components.Notification;

public class CleanupTask implements ITask {

    private long nextTime;
    private final long interval;
    private static final Predicate<Entity> ENTITY_PREDICATE = entity -> {
        ServerUtilitiesConfig.Tasks.Cleanup config = ServerUtilitiesConfig.tasks.cleanup;
        if (entity instanceof EntityPlayer) return false;
        if ((entity instanceof IAnimals && !(entity instanceof IMob)) || entity instanceof INpc) {
            return config.passives;
        }
        if (entity instanceof IMob) {
            return config.hostiles;
        }
        if (entity instanceof EntityItem) {
            return config.items;
        }
        return config.experience && entity instanceof EntityXPOrb;
    };

    public CleanupTask(double interval) {
        this.interval = Ticks.HOUR.x(interval).millis();
        nextTime = System.currentTimeMillis() + this.interval;
        queueNotifications(Universe.get());
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
    public void execute(Universe universe) {
        int removed = 0;
        for (World world : universe.server.worldServers) {
            for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
                if (ENTITY_PREDICATE.test(entity)) {
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

    @Override
    public void queueNotifications(Universe universe) {
        if (ServerUtilitiesConfig.tasks.cleanup.silent) return;
        IChatComponent component = getNotificationString(30);
        ITask task = new NotifyTask(nextTime - Ticks.SECOND.x(30).millis(), "cleanup_30", component);

        universe.scheduleTask(task);

        component = getNotificationString(60);
        task = new NotifyTask(nextTime - Ticks.SECOND.x(60).millis(), "cleanup_60", component);

        universe.scheduleTask(task);
    }

    private IChatComponent getNotificationString(int seconds) {
        ServerUtilitiesConfig.Tasks.Cleanup config = ServerUtilitiesConfig.tasks.cleanup;
        StringBuilder builder = new StringBuilder();
        if (config.hostiles) {
            builder.append(StatCollector.translateToLocal("serverutilities.task.cleanup_hostiles"));
        }
        if (config.passives) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(StatCollector.translateToLocal("serverutilities.task.cleanup_passives"));
        }
        if (config.items) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(StatCollector.translateToLocal("serverutilities.task.cleanup_items"));
        }
        if (config.experience) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(StatCollector.translateToLocal("serverutilities.task.cleanup_experience"));
        }

        int index = builder.lastIndexOf(",");

        if (index > 0) {
            builder.replace(index, index + 1, " &");
        }

        String finalString = StatCollector
                .translateToLocalFormatted("serverutilities.task.cleanup_entity", builder.toString().toLowerCase(), seconds);

        return StringUtils.color(new ChatComponentText(finalString), EnumChatFormatting.LIGHT_PURPLE);
    }
}
