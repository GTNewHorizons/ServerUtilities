package serverutils.lib.util;

import java.io.InterruptedIOException;

import lombok.Getter;

public class CancellationToken {

    @Getter
    private volatile boolean cancelled;
    private volatile Thread runner;

    public void enable() {
        runner = Thread.currentThread();
        if (cancelled) runner.interrupt();
    }

    public void disable() {
        runner = null;
    }

    public void cancel() {
        cancelled = true;
        Thread t = runner;
        if (t != null && t != Thread.currentThread()) t.interrupt();
    }

    public void check() throws InterruptedIOException {
        if (cancelled || Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
    }

    public void clear() {
        cancelled = false;
        runner = null;
    }
}
