package serverutils;

import net.minecraft.world.World;

public interface ServerUtilitiesGameRules {

    String DISABLE_TEAM_CREATION = "serverutilities:disable_team_creation";
    String DISABLE_TEAM_JOINING = "serverutilities:disable_team_joining";

    static boolean canCreateTeam(World world) {
        return !ServerUtilitiesConfig.teams.disable_teams
                && !world.getGameRules().getGameRuleBooleanValue(DISABLE_TEAM_CREATION);
    }

    static boolean canJoinTeam(World world) {
        return !ServerUtilitiesConfig.teams.disable_teams
                && !world.getGameRules().getGameRuleBooleanValue(DISABLE_TEAM_JOINING);
    }
}
