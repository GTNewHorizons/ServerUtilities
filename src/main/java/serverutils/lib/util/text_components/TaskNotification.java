package serverutils.lib.util.text_components;

public class TaskNotification extends Notification {

    private long nextTime;

    public TaskNotification(Notification notification) {
        super(notification);
    }

    public long getNextTime() {
        return nextTime;
    }

    public TaskNotification setNextTime(long time) {
        nextTime = time;
        return this;
    }
}
