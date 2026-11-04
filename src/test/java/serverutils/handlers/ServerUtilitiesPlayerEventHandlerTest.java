package serverutils.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;
import org.mockito.MockedStatic;

import serverutils.ServerUtilitiesConfig;
import serverutils.data.ServerUtilitiesTeamData;
import serverutils.events.player.ForgePlayerLoggedInEvent;
import serverutils.events.player.ForgePlayerLoggedOutEvent;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.task.backup.BackupTask;

public class ServerUtilitiesPlayerEventHandlerTest {

    @Test
    public void loginAndLogout_ShouldRecordPlayerActivity() {
        ServerUtilitiesConfig.chat.replace_tab_names = false;

        EntityPlayerMP player = mock(EntityPlayerMP.class);
        when(player.getEntityData()).thenReturn(new NBTTagCompound());
        ForgePlayer forgePlayer = mock(ForgePlayer.class);
        forgePlayer.team = mock(ForgeTeam.class);
        when(forgePlayer.getPlayer()).thenReturn(player);

        ForgePlayerLoggedInEvent login = mock(ForgePlayerLoggedInEvent.class);
        when(login.getPlayer()).thenReturn(forgePlayer);
        ForgePlayerLoggedOutEvent logout = mock(ForgePlayerLoggedOutEvent.class);
        when(logout.getPlayer()).thenReturn(forgePlayer);

        try (MockedStatic<BackupTask> backupTask = mockStatic(BackupTask.class);
                MockedStatic<ServerUtilitiesTeamData> ignored = mockStatic(ServerUtilitiesTeamData.class)) {
            ServerUtilitiesPlayerEventHandler.onPlayerLoggedIn(login);
            backupTask.verify(BackupTask::newPlayerActivity, times(1));

            ServerUtilitiesPlayerEventHandler.onPlayerLoggedOut(logout);
            backupTask.verify(BackupTask::newPlayerActivity, times(2));
        }
    }
}
