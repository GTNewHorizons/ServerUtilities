package serverutils.lib.util.misc;

import serverutils.lib.data.Universe;

@FunctionalInterface
public interface IScheduledTask {

    default boolean isComplete(Universe universe, TimeType type, long time) {
        return (type == TimeType.TICKS ? universe.ticks.ticks() : System.currentTimeMillis()) >= time;
    }

    void execute(Universe universe);
}
