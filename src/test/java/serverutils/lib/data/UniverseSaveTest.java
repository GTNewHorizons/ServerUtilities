package serverutils.lib.data;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.world.WorldEvent;

import org.junit.Test;

public class UniverseSaveTest {

    @Test
    public void failuresRetryOncePerTickButBackupAndShutdownBypassTheLimit() throws Exception {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getTickCounter()).thenReturn(100);
        Universe universe = new Universe(server);
        File failed = mock(File.class);
        File successful = mock(File.class);
        when(failed.exists()).thenReturn(true);
        when(successful.exists()).thenReturn(true);
        when(successful.delete()).thenReturn(true);
        ForgeTeam retry = new ForgeTeam(universe, (short) 3, "retry", TeamType.SERVER_NO_SAVE) {

            @Override
            public File getDataFile(String extension) {
                return failed;
            }
        };
        ForgeTeam saved = new ForgeTeam(universe, (short) 4, "saved", TeamType.SERVER_NO_SAVE) {

            @Override
            public File getDataFile(String extension) {
                return successful;
            }
        };
        universe.addTeam(retry);
        universe.addTeam(saved);
        retry.markDirty();
        saved.markDirty();
        Field instance = Universe.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        Object previous = instance.get(null);
        instance.set(null, universe);
        try {
            for (int dimension = 0; dimension < 62; dimension++) universe.onWorldSaved(new WorldEvent.Save(null));
            verify(failed, times(1)).delete();
            verify(successful, times(1)).delete();
            assertTrue(retry.needsSaving);
            assertFalse(saved.needsSaving);
            assertTrue(universe.checkSaving);

            when(server.getTickCounter()).thenReturn(101);
            universe.onWorldSaved(new WorldEvent.Save(null));
            verify(failed, times(2)).delete();
            assertThrows(IOException.class, universe::saveForBackup);
            verify(failed, times(3)).delete();

            when(failed.delete()).thenReturn(true);
            universe.saveForBackup();
            verify(failed, times(4)).delete();
            assertFalse(retry.needsSaving);
            assertFalse(universe.checkSaving);
            universe.onWorldSaved(new WorldEvent.Save(null));
            verify(failed, times(4)).delete();

            // New dirty data after a successful save must still save in the same tick.
            retry.markDirty();
            universe.onWorldSaved(new WorldEvent.Save(null));
            verify(failed, times(5)).delete();
            when(failed.delete()).thenReturn(false);
            retry.markDirty();
            universe.onWorldSaved(new WorldEvent.Save(null));
            verify(failed, times(6)).delete();
            when(failed.delete()).thenReturn(true);
            Universe.onServerStopping(null);
            verify(failed, times(7)).delete();
            verify(successful, times(1)).delete();
            assertFalse(retry.needsSaving);
            assertNull(Universe.getNullable());
        } finally {
            instance.set(null, previous);
        }
    }
}
