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
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import serverutils.lib.data.Universe;
import serverutils.task.backup.BackupFixtures;

public class ServerUtilitiesServerEventHandlerTest {

    private Universe universe;

    @Before
    public void beforeEach() throws ReflectiveOperationException {
        MinecraftServer server = mock(MinecraftServer.class);
        server.worldServers = new WorldServer[] { mock(WorldServer.class) };
        universe = new Universe(server);
        BackupFixtures.setStatic(Universe.class, "INSTANCE", universe);
    }

    @After
    public void afterEach() throws ReflectiveOperationException {
        universe.restoreWorldSaving();
        BackupFixtures.setStatic(Universe.class, "INSTANCE", null);
    }

    @Test
    public void loadWorldEvent_DuringBackup_SuspendsSavingUntilRestored() throws Exception {
        WorldServer loadedLater = mock(WorldServer.class);
        Field provider = World.class.getDeclaredField("provider");
        provider.setAccessible(true);
        provider.set(loadedLater, mock(WorldProvider.class));

        universe.disableWorldSaving();
        ServerUtilitiesServerEventHandler.loadWorldEvent(new WorldEvent.Load(loadedLater));
        assertTrue(loadedLater.levelSaving);

        universe.restoreWorldSaving();
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
        universe.disableWorldSaving();

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