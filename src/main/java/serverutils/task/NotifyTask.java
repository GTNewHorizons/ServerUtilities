package serverutils.task;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;

import serverutils.lib.data.Universe;
import serverutils.lib.util.text_components.Notification;

public class NotifyTask extends Task {

    private final Notification notification;
    private final EntityPlayerMP player;

    public NotifyTask(long whenToRun, @Nullable EntityPlayerMP player, Notification notification) {
        super(whenToRun);
        this.player = player;
        this.notification = notification;
    }

    public NotifyTask(long whenToRun, Notification notification) {
        this(whenToRun, null, notification);
    }

    @Override
    public void execute(Universe universe) {
        if (player == null) {
            notification.sendToAll();
        } else {
            notification.send(player);
        }
    }
}
