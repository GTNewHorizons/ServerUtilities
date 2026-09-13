package serverutils.data;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import cpw.mods.fml.common.eventhandler.EventBus;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.IEventListener;
import cpw.mods.fml.common.eventhandler.ListenerList;
import serverutils.events.team.ForgeTeamSavedEvent;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.TeamType;
import serverutils.lib.data.Universe;
import serverutils.lib.util.NBTUtils;

public class ClaimSaveTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void failedClaimWriteAndDeleteKeepTeamDirtyUntilRetry() throws Exception {
        Universe universe = new Universe(mock(MinecraftServer.class));
        universe.dataFolder = temporary.newFolder();
        ForgeTeam team = new ForgeTeam(universe, (short) 1, "test", TeamType.SERVER);
        universe.addTeam(team);
        Path target = team.getDataFile("claimedchunks").toPath();
        Files.createDirectories(target);
        Path blocker = Files.write(target.resolve("keep"), new byte[] { 1 });
        NBTTagCompound data = new NBTTagCompound();
        data.setString("claims", "saved");
        IEventListener subscriber = event -> {
            if (event instanceof ForgeTeamSavedEvent saved) {
                ServerUtilitiesTeamData.saveClaimData(saved, target.toFile(), data);
            }
        };
        // Register directly: the annotation registration path requires Forge's game classloader.
        java.lang.reflect.Field busIdField = EventBus.class.getDeclaredField("busID");
        busIdField.setAccessible(true);
        int busId = busIdField.getInt(MinecraftForge.EVENT_BUS);
        ListenerList listeners = new ForgeTeamSavedEvent(team).getListenerList();
        listeners.register(busId, EventPriority.NORMAL, subscriber);
        Method save = Universe.class.getDeclaredMethod("save");
        save.setAccessible(true);
        try {
            team.markDirty();
            save.invoke(universe);
            assertTrue(team.needsSaving);
            Files.delete(blocker);
            Files.delete(target);
            save.invoke(universe);
            assertFalse(team.needsSaving);
            assertEquals("saved", NBTUtils.readNBT(target.toFile()).getString("claims"));

            Files.delete(target);
            Files.createDirectory(target);
            blocker = Files.write(target.resolve("keep"), new byte[] { 1 });
            data.removeTag("claims");
            team.markDirty();
            save.invoke(universe);
            assertTrue(team.needsSaving);
            Files.delete(blocker);
            save.invoke(universe);
            assertFalse(team.needsSaving);
            assertFalse(Files.exists(target));
        } finally {
            listeners.unregister(busId, subscriber);
        }
    }
}
