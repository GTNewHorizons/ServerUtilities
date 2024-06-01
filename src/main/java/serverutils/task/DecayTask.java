package serverutils.task;

import static serverutils.ServerUtilitiesConfig.world;
import static serverutils.ServerUtilitiesPermissions.CHUNKLOAD_DECAY_TIMER;
import static serverutils.ServerUtilitiesPermissions.CLAIM_DECAY_TIMER;

import java.util.OptionalInt;

import serverutils.ServerUtilities;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesTeamData;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;

public class DecayTask extends Task {

    public DecayTask() {
        super(Ticks.MINUTE.x(5));
    }

    @Override
    public void execute(Universe universe) {
        if (!ClaimedChunks.isActive()) return;
        for (ForgeTeam team : universe.getTeams()) {
            if (checkDecay(team, CLAIM_DECAY_TIMER)) {
                ClaimedChunks.instance.unclaimAllChunks(null, team, OptionalInt.empty());
                ServerUtilities.LOGGER.info("Decaying claimed chunks for {}", team.getId());
                continue;
            }

            ServerUtilitiesTeamData data = ServerUtilitiesTeamData.get(team);
            if (data.chunkloadsDecayed || !world.chunk_loading) continue;

            if (checkDecay(team, CHUNKLOAD_DECAY_TIMER)) {
                data.decayChunkloads();
                ServerUtilities.LOGGER.info("Decaying loaded chunks for {}", team.getId());
            }
        }
    }

    public boolean checkDecay(ForgeTeam team, String node) {
        ServerUtilitiesTeamData data = ServerUtilitiesTeamData.get(team);
        if (!team.isValid() || data.getTeamChunks().isEmpty()) return false;

        if (!team.getOnlineMembers().isEmpty()) {
            team.refreshActivity();
            return false;
        }

        long highestTimer = team.getHighestTimer(node).millis();
        long latestLogin = team.getLastActivity();

        if (latestLogin <= 0 || highestTimer <= 0) return false;

        return System.currentTimeMillis() >= latestLogin + highestTimer;
    }
}
