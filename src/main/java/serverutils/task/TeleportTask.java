package serverutils.task;

import static serverutils.ServerUtilitiesNotifications.TELEPORT_WARMUP;

import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.data.Universe;
import serverutils.lib.math.BlockDimPos;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.text_components.Notification;

public class TeleportTask extends Task {

    private final EntityPlayerMP player;
    private final ServerUtilitiesPlayerData.Timer timer;
    private final BlockDimPos startPos;
    private final Function<EntityPlayerMP, TeleporterDimPos> pos;
    private final float startHP;
    private final int startSeconds;
    private int secondsLeft;
    private final Task extraTask;
    private final TeleportType teleportType;

    public TeleportTask(TeleportType teleportType, EntityPlayerMP player, ServerUtilitiesPlayerData.Timer ticks,
            int secStart, Function<EntityPlayerMP, TeleporterDimPos> to, @Nullable Task task) {
        super(0);
        this.teleportType = teleportType;
        this.player = player;
        this.timer = ticks;
        this.startPos = new BlockDimPos(player);
        this.startHP = player.getHealth();
        this.pos = to;
        this.startSeconds = secStart;
        this.secondsLeft = secStart;
        this.extraTask = task;
    }

    @Override
    public void execute(Universe universe) {
        if (!startPos.equalsPos(new BlockDimPos(player)) || startHP > player.getHealth()) {
            player.addChatMessage(
                    StringUtils.color(ServerUtilities.lang(player, "stand_still_failed"), EnumChatFormatting.RED));
        } else if (secondsLeft <= 1) {
            TeleporterDimPos teleporter = pos.apply(player);
            if (teleporter != null) {
                Entity mount = player.ridingEntity;
                player.mountEntity(null);
                if (mount != null) {
                    teleporter.teleport(mount);
                }

                ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(universe.getPlayer(player));
                data.setLastTeleport(teleportType, new BlockDimPos(player));
                teleporter.teleport(player);

                data.lastTeleport[timer.ordinal()] = System.currentTimeMillis();

                if (secondsLeft != 0) {
                    player.addChatMessage(ServerUtilities.lang(player, "teleporting"));
                }

                if (extraTask != null) {
                    extraTask.execute(universe);
                }
            }
        } else {
            secondsLeft -= 1;
            setNextTime(System.currentTimeMillis() + Ticks.SECOND.millis());
            universe.scheduleTask(this);

            IChatComponent component = StringUtils.color(
                    ServerUtilities.lang(player, "stand_still", startSeconds).appendText(" [" + secondsLeft + "]"),
                    EnumChatFormatting.GOLD);

            Notification.of(TELEPORT_WARMUP, component).setVanilla(true).send(player.mcServer, player);
        }
    }
}
