package serverutils.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandSaveAll;
import net.minecraft.command.server.CommandSaveOff;
import net.minecraft.command.server.CommandSaveOn;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.junit.After;
import org.junit.Test;

import serverutils.task.backup.BackupFixtures;

public class ServerUtilitiesServerEventHandlerTest {

    @After
    public void afterEach() {
        BackupFixtures.resumeWorldSaving();
    }

    @Test
    public void loadWorldEvent_DuringBackup_SuspendsSavingUntilRestored() throws Exception {
        WorldServer loadedLater = mock(WorldServer.class);
        Field provider = World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(loadedLater, mock(WorldProvider.class));

        BackupFixtures.suspendWorldSaving(mock(WorldServer.class));
        ServerUtilitiesServerEventHandler.loadWorldEvent(new WorldEvent.Load(loadedLater));
        assertTrue(loadedLater.levelSaving);

        BackupFixtures.resumeWorldSaving();
        assertFalse(loadedLater.levelSaving);
    }

    @Test
    public void loadWorldEvent_OutsideBackup_LeavesSavingAlone() {
        WorldServer loaded = mock(WorldServer.class);

        ServerUtilitiesServerEventHandler.loadWorldEvent(new WorldEvent.Load(loaded));

        assertFalse(loaded.levelSaving);
    }

    @Test
    public void onBackupSaveCommand_DuringBackup_RejectsSaveCommands() throws Exception {
        BackupFixtures.suspendWorldSaving(mock(WorldServer.class));

        for (ICommand command : new ICommand[] { new CommandSaveAll(), new CommandSaveOn(), new CommandSaveOff() }) {
            CommandEvent event = spy(new CommandEvent(command, mock(ICommandSender.class), new String[0]));
            when(event.isCancelable()).thenReturn(true);

            ServerUtilitiesServerEventHandler.onBackupSaveCommand(event);

            assertTrue(event.isCanceled());
            assertTrue(event.exception instanceof CommandException);
        }
    }

    @Test
    public void onBackupSaveCommand_OutsideBackup_AllowsSaveCommands() {
        CommandEvent event = new CommandEvent(new CommandSaveAll(), mock(ICommandSender.class), new String[0]);

        ServerUtilitiesServerEventHandler.onBackupSaveCommand(event);

        assertFalse(event.isCanceled());
    }
}
