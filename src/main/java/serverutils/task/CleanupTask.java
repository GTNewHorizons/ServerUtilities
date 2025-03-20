package serverutils.task;

import static serverutils.ServerUtilitiesConfig.tasks;
import static serverutils.ServerUtilitiesNotifications.CLEANUP;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.INpc;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.StringUtils;

public class CleanupTask extends Task {

    public CleanupTask() {
        super(Ticks.HOUR.x(tasks.cleanup.interval));
    }

    @Override
    public void execute(Universe universe) {
        int removed = 0;
        for (World world : universe.server.worldServers) {
            for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
                if (shouldDespawn(entity)) {
                    entity.setDead();
                    removed++;
                }
            }
        }

        CLEANUP.sendAll("serverutilities.task.cleanup_removed", removed);
    }

    @Override
    public List<NotifyTask> getNotifications() {
        List<NotifyTask> notifications = new ArrayList<>();
        if (tasks.cleanup.silent) return notifications;
        notifications.add(
                new NotifyTask(
                        nextTime - Ticks.SECOND.x(30).millis(),
                        CLEANUP.createNotification(getNotificationString(30))));
        notifications.add(
                new NotifyTask(
                        nextTime - Ticks.SECOND.x(60).millis(),
                        CLEANUP.createNotification(getNotificationString(60))));
        return notifications;
    }

    private IChatComponent getNotificationString(int seconds) {
        ServerUtilitiesConfig.Tasks.Cleanup config = tasks.cleanup;
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

        String finalString = StatCollector.translateToLocalFormatted(
                "serverutilities.task.cleanup_entity",
                builder.toString().toLowerCase(),
                seconds);

        return StringUtils.color(new ChatComponentText(finalString), EnumChatFormatting.LIGHT_PURPLE);
    }

    private static boolean shouldDespawn(Entity entity) {
        ServerUtilitiesConfig.Tasks.Cleanup config = tasks.cleanup;
        if (entity instanceof EntityLiving living && living.isNoDespawnRequired()) {
            return false;
        }

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
    }
}
