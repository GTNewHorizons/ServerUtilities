package serverutils.task;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IChatComponent;

import serverutils.lib.data.Universe;
import serverutils.lib.util.text_components.Notification;

public class NotifyTask extends SimpleTask {

    private final Notification notification;
    private final EntityPlayerMP player;

    public NotifyTask(long whenToRun, @Nullable EntityPlayerMP player, String id, IChatComponent... message) {
        super(whenToRun);
        this.player = player;
        notification = Notification.of(id, message);
    }

    public NotifyTask(long whenToRun, String id, IChatComponent... message) {
        this(whenToRun, null, id, message);
    }

    @Override
    public void execute(Universe universe) {
        if (player == null) {
            notification.sendToAll(universe.server);
        } else {
            notification.send(universe.server, player);
        }
    }
}
