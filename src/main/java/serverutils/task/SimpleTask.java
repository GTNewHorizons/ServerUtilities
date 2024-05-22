package serverutils.task;

import serverutils.lib.data.Universe;

public abstract class SimpleTask implements ITask {

    private final long runIn;

    public SimpleTask(long runIn) {
        this.runIn = runIn;
    }

    public SimpleTask() {
        this(-1);
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public long getNextTime() {
        return runIn;
    }

    @Override
    public long getInterval() {
        return 0;
    }

    @Override
    public void setNextTime(long time) {}

    public abstract void execute(Universe universe);
}
