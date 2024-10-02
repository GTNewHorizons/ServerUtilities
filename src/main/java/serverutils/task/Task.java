package serverutils.task;

import java.util.List;

import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.misc.TimeType;

public abstract class Task {

    protected long nextTime;
    protected long interval;
    protected boolean repeatable;

    public Task() {
        this(-1, Ticks.NO_TICKS, false);
    }

    public Task(Ticks interval) {
        this(System.currentTimeMillis(), interval, true);
    }

    public Task(long whenToRun) {
        this(whenToRun, Ticks.NO_TICKS, false);
    }

    public Task(long whenToRun, Ticks ticks, boolean repeatable) {
        this.interval = getTimeType() == TimeType.MILLIS ? ticks.millis() : ticks.ticks();
        this.nextTime = whenToRun + interval;
        this.repeatable = repeatable;
    }

    public abstract void execute(Universe universe);

    public boolean isRepeatable() {
        return repeatable;
    }

    public long getNextTime() {
        return nextTime;
    }

    public long getInterval() {
        return interval;
    }

    public void setNextTime(long time) {
        this.nextTime = time;
        queueNotifications(Universe.get());
    }

    public void queueNotifications(Universe universe) {
        List<NotifyTask> notifications = getNotifications();
        if (notifications == null || notifications.isEmpty()) return;
        getNotifications().forEach(universe::scheduleTask);
    }

    protected List<NotifyTask> getNotifications() {
        return null;
    }

    public boolean isComplete(Universe universe) {
        return (getTimeType() == TimeType.TICKS ? universe.ticks.ticks() : System.currentTimeMillis()) >= getNextTime();
    }

    protected TimeType getTimeType() {
        return TimeType.MILLIS;
    }
}
