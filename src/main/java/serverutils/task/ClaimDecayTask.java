package serverutils.task;

import java.util.OptionalInt;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesPermissions;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesTeamData;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;

public class ClaimDecayTask extends Task {

    public ClaimDecayTask() {
        super(Ticks.MINUTE.x(5));
    }

    @Override
    public void execute(Universe universe) {
        for (ForgeTeam team : universe.getTeams()) {
            ServerUtilitiesTeamData data = ServerUtilitiesTeamData.get(team);
            if (!team.isValid() || data.getTeamChunks().isEmpty()) continue;

            if (!team.getOnlineMembers().isEmpty()) {
                team.refreshActivity();
                continue;
            }

            long highestTimer = team.getHighestTimer(ServerUtilitiesPermissions.CLAIM_DECAY_TIMER).millis();
            long latestLogin = team.getLastActivity();

            if (latestLogin <= 0 || highestTimer <= 0) continue;

            if (System.currentTimeMillis() >= latestLogin + highestTimer) {
                ClaimedChunks.instance.unclaimAllChunks(null, team, OptionalInt.empty());
                ServerUtilities.LOGGER.info("Decaying claimed chunks for {}", team.getId());
            }
        }
    }
}
