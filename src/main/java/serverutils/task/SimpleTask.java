package serverutils.task;

import serverutils.lib.data.Universe;

public abstract class SimpleTask implements ITask {

    private final long time;

    public SimpleTask(long whenToRun) {
        this.time = whenToRun;
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
        return time;
    }

    @Override
    public long getInterval() {
        return 0;
    }

    @Override
    public void setNextTime(long time) {}

    public abstract void execute(Universe universe);
}
