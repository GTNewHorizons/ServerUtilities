package serverutils.serverlib.lib.util.misc;

import serverutils.serverlib.lib.data.Universe;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface IScheduledTask
{
	default boolean isComplete(Universe universe, TimeType type, long time)
	{
		return (type == TimeType.TICKS ? universe.ticks.ticks() : System.currentTimeMillis()) >= time;
	}

	void execute(Universe universe);
}