package serverutils.lib.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;

import org.junit.Before;
import org.junit.Test;

public class UniverseTest {

    private WorldServer world;
    private Universe universe;

    @Before
    public void beforeEach() {
        world = mock(WorldServer.class);
        MinecraftServer server = mock(MinecraftServer.class);
        server.worldServers = new WorldServer[] { world, null };
        universe = new Universe(server);
    }

    @Test
    public void disableWorldSaving_WhenSavingAlreadyOff_StillSavesAndKeepsItOff() throws MinecraftException {
        world.levelSaving = true;
        doAnswer(invocation -> {
            assertFalse("Backup must force a current chunk save", world.levelSaving);
            return null;
        }).when(world).saveAllChunks(true, null);

        universe.disableWorldSaving();
        verify(world).saveAllChunks(true, null);
        assertTrue(world.levelSaving);
        assertTrue(universe.isWorldSavingSuspended());

        universe.restoreWorldSaving();
        assertTrue("Saving was off before the backup, so it stays off", world.levelSaving);
        assertFalse(universe.isWorldSavingSuspended());
    }

    @Test
    public void disableWorldSaving_WhenSavingOn_RestoresItAfterwards() throws MinecraftException {
        world.levelSaving = false;

        universe.disableWorldSaving();
        assertTrue(world.levelSaving);

        universe.restoreWorldSaving();
        assertFalse(world.levelSaving);
    }

    @Test
    public void disableWorldSaving_WhenSaveFails_RestoresBeforeThrowing() throws MinecraftException {
        world.levelSaving = false;
        MinecraftException failure = new MinecraftException("save failed");
        doThrow(failure).when(world).saveAllChunks(true, null);

        assertSame(failure, assertThrows(MinecraftException.class, universe::disableWorldSaving));
        assertFalse(world.levelSaving);
        assertFalse(universe.isWorldSavingSuspended());
    }
}