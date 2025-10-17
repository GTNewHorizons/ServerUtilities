package serverutils.task;

import static serverutils.ServerUtilitiesNotifications.TELEPORT_WARMUP;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.lib.math.BlockDimPos;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.StringUtils;

public class TeleportTask extends Task {

    private final UUID playerUUID;
    private final BlockDimPos startPos;
    private final TeleporterDimPos teleporter;
    private final float startHP;
    private final int startSeconds;
    private int secondsLeft;
    private final Task extraTask;
    private final TeleportType teleportType;

    public TeleportTask(TeleportType teleportType, EntityPlayerMP player, int secStart, TeleporterDimPos to,
            @Nullable Task task) {
        super(0);
        this.teleportType = teleportType;
        this.playerUUID = player.getUniqueID();
        this.startPos = new BlockDimPos(player);
        this.startHP = player.getHealth();
        this.teleporter = to;
        this.startSeconds = secStart;
        this.secondsLeft = secStart;
        this.extraTask = task;
    }

    @Override
    public void execute(Universe universe) {
        ForgePlayer fPlayer = universe.getPlayer(playerUUID);
        if (fPlayer == null) return;
        var player = fPlayer.getNullablePlayer();
        if (player == null) return;

        if (!startPos.equalsPos(new BlockDimPos(player)) || startHP > player.getHealth()) {
            player.addChatMessage(StringUtils.color("serverutilities.lang.warps.cancelled", EnumChatFormatting.RED));
        } else if (secondsLeft <= 1) {
            Entity mount = player.ridingEntity;
            player.mountEntity(null);
            if (mount != null) {
                teleporter.teleport(mount);
            }

            ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(universe.getPlayer(player));
            data.setLastTeleport(teleportType, new BlockDimPos(player));
            teleporter.teleport(player);

            if (secondsLeft != 0) {
                player.addChatMessage(ServerUtilities.lang(player, "teleporting"));
            }

            if (extraTask != null) {
                extraTask.execute(universe);
            }
        } else {
            secondsLeft -= 1;
            setNextTime(System.currentTimeMillis() + Ticks.SECOND.millis());
            IChatComponent component = StringUtils.color(
                    ServerUtilities.lang("stand_still", startSeconds).appendText(" [" + secondsLeft + "]"),
                    EnumChatFormatting.GOLD);
            TELEPORT_WARMUP.createNotification(component).setVanilla(true).send(player);
            universe.scheduleTask(this);
        }
    }
}
