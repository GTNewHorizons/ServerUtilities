package serverutils.command;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cpw.mods.fml.common.gameevent.TickEvent;
import serverutils.ServerUtilitiesConfig;
import serverutils.handlers.ServerUtilitiesServerEventHandler;

public class CmdBackupThreadTest {

    private int originalTimeout;

    @Before
    public void setUp() {
        originalTimeout = ServerUtilitiesConfig.backups.external_hold_prepare_timeout_seconds;
        ServerUtilitiesConfig.backups.external_hold_prepare_timeout_seconds = 5;
        ServerUtilitiesServerEventHandler.clearServerTasks();
    }

    @After
    public void tearDown() {
        ServerUtilitiesServerEventHandler.clearServerTasks();
        ServerUtilitiesConfig.backups.external_hold_prepare_timeout_seconds = originalTimeout;
    }

    @Test
    public void rconWorkRunsOnServerTickAndReturnsToCaller() throws Exception {
        AtomicReference<Thread> executionThread = new AtomicReference<>();
        AtomicReference<IChatComponent> reply = new AtomicReference<>();
        Thread caller = new Thread(() -> reply.set(CmdBackup.runBackupCommand(true, () -> {
            executionThread.set(Thread.currentThread());
            return new ChatComponentText("finished");
        })));
        caller.start();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!ServerUtilitiesServerEventHandler.hasScheduledServerTasks() && System.nanoTime() < deadline) {
            Thread.yield();
        }
        assertTrue(ServerUtilitiesServerEventHandler.hasScheduledServerTasks());
        assertNull(executionThread.get());
        ServerUtilitiesServerEventHandler.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        caller.join(5000);
        assertFalse(caller.isAlive());
        assertSame(Thread.currentThread(), executionThread.get());
        assertEquals("finished", reply.get().getUnformattedText());
    }

    @Test
    public void consoleWorkRunsInlineWithoutQueueing() {
        Thread caller = Thread.currentThread();
        assertEquals("finished", CmdBackup.runBackupCommand(false, () -> {
            assertSame(caller, Thread.currentThread());
            return new ChatComponentText("finished");
        }).getUnformattedText());
        assertFalse(ServerUtilitiesServerEventHandler.hasScheduledServerTasks());
    }

    @Test
    public void timedOutQueuedCommandDoesNotExecuteLater() {
        ServerUtilitiesConfig.backups.external_hold_prepare_timeout_seconds = 1;
        AtomicBoolean executed = new AtomicBoolean();
        CmdBackup.runBackupCommand(true, () -> {
            executed.set(true);
            return new ChatComponentText("late reply");
        });
        ServerUtilitiesServerEventHandler.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
        assertFalse(executed.get());
        assertFalse(ServerUtilitiesServerEventHandler.hasScheduledServerTasks());
    }
}
