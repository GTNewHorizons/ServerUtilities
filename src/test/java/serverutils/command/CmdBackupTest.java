package serverutils.command;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cpw.mods.fml.common.gameevent.TickEvent;
import serverutils.handlers.ServerUtilitiesServerEventHandler;
import serverutils.lib.data.Universe;
import serverutils.task.backup.BackupFixtures;

public class CmdBackupTest extends BackupFixtures.Tests {

    @Before
    public void beforeEach() {
        testSetup();
        ServerUtilitiesServerEventHandler.clearServerTasks();
    }

    @After
    public void afterEach() throws ReflectiveOperationException {
        ServerUtilitiesServerEventHandler.clearServerTasks();
        BackupFixtures.setStatic(Universe.class, "INSTANCE", null);
        testReset();
    }

    @Test
    public void start_WhenPreparationFails_ReportsFailureWithoutLaunchMessage() throws Exception {
        BackupFixtures.setStatic(Universe.class, "INSTANCE", universe);
        ServerConfigurationManager manager = server.getConfigurationManager();
        doThrow(new IllegalStateException("preparation failed")).when(manager).saveAllPlayerData();
        List<String> keys = new ArrayList<>();
        ICommandSender sender = recordingSender(keys);

        new CmdBackup.CmdBackupStart("start").processCommand(sender, new String[] { "checkpoint" });

        assertTrue(keys.contains("cmd.backup_manual_failed"));
        assertFalse(keys.stream().anyMatch(key -> key.startsWith("cmd.backup_manual_launch")));
    }

    @Test
    public void start_WhenNameTaken_RejectsBeforeSavingAnything() throws Exception {
        BackupFixtures.setStatic(Universe.class, "INSTANCE", universe);
        File taken = new File(backups, "checkpoint.zip");
        byte[] contents = { 1, 2, 3 };
        Files.write(taken.toPath(), contents);
        ServerConfigurationManager manager = server.getConfigurationManager();

        assertThrows(
                WrongUsageException.class,
                () -> new CmdBackup.CmdBackupStart("start")
                        .processCommand(mock(ICommandSender.class), new String[] { "checkpoint" }));

        verify(manager, never()).saveAllPlayerData();
        assertArrayEquals(contents, Files.readAllBytes(taken.toPath()));
    }

    @Test
    public void list_ForPlayer_RepliesOnServerTickInCreationOrder() throws Exception {
        String worldId = UUID.randomUUID().toString();
        File old = new File(backups, "list-old.zip");
        File recent = new File(backups, "list-recent.zip");
        BackupFixtures.writeArchive(old, worldId, 1_000);
        BackupFixtures.writeArchive(recent, worldId, 2_000);
        // Reversed modification times, so sorting by them would put recent first.
        Files.setLastModifiedTime(old.toPath(), FileTime.fromMillis(9_000));
        Files.setLastModifiedTime(recent.toPath(), FileTime.fromMillis(1));

        List<ChatComponentTranslation> replies = new ArrayList<>();
        Thread caller = Thread.currentThread();
        ICommandSender player = mock(ICommandSender.class);
        doAnswer(call -> {
            assertSame("Player replies must run on the server thread", caller, Thread.currentThread());
            replies.add(call.getArgument(0));
            return null;
        }).when(player).addChatMessage(any());

        new CmdBackup.CmdBackupList().processCommand(player, new String[0]);
        assertTrue("Player replies must wait for the server task queue", replies.isEmpty());
        tickUntil(() -> !replies.isEmpty());

        assertEquals("cmd.backup_list_header", replies.get(0).getKey());
        assertEquals(2, replies.get(0).getFormatArgs()[0]);
        List<Object> names = replies.stream().filter(reply -> reply.getKey().equals("cmd.backup_list_file"))
                .map(reply -> reply.getFormatArgs()[0]).collect(Collectors.toList());
        List<Object> expected = new ArrayList<>();
        expected.add(old.getName());
        expected.add(recent.getName());
        assertEquals(expected, names);
    }

    @Test
    public void list_ForRcon_RepliesBeforeReturning() throws Exception {
        BackupFixtures.writeArchive(new File(backups, "list.zip"), UUID.randomUUID().toString(), 1_000);
        RConConsoleSource rcon = mock(RConConsoleSource.class);

        new CmdBackup.CmdBackupList().processCommand(rcon, new String[0]);

        verify(rcon, atLeastOnce()).addChatMessage(any());
    }

    private static ICommandSender recordingSender(List<String> keys) {
        ICommandSender sender = mock(ICommandSender.class);
        doAnswer(call -> {
            IChatComponent message = call.getArgument(0);
            if (message instanceof ChatComponentTranslation translation) keys.add(translation.getKey());
            return null;
        }).when(sender).addChatMessage(any());
        return sender;
    }

    /** Runs server ticks until the condition holds; replies arrive once the background scan finishes. */
    private static void tickUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            assertTrue("Timed out waiting for server task", System.nanoTime() < deadline);
            ServerUtilitiesServerEventHandler.onServerTick(new TickEvent.ServerTickEvent(TickEvent.Phase.START));
            Thread.sleep(10);
        }
    }
}
