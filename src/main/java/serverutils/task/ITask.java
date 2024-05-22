package serverutils.task;

import serverutils.lib.data.Universe;
import serverutils.lib.util.misc.TimeType;

public interface ITask {

    boolean isRepeatable();

    long getNextTime();

    long getInterval();

    void setNextTime(long time);

    void execute(Universe universe);

    default void notify(Universe universe) {}

    default boolean isComplete(Universe universe) {
        return (getTimeType() == TimeType.TICKS ? universe.ticks.ticks() : System.currentTimeMillis()) >= getNextTime();
    }

    default TimeType getTimeType() {
        return TimeType.MILLIS;
    }
}
