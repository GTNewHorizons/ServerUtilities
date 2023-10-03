package serverutils.lib;

import net.minecraft.world.World;

import serverutils.mod.ServerUtilitiesConfig;

public interface ServerUtilitiesLibGameRules {

    String DISABLE_TEAM_CREATION = "serverlib:disable_team_creation";
    String DISABLE_TEAM_JOINING = "serverlib:disable_team_joining";

    static boolean canCreateTeam(World world) {
        return !ServerUtilitiesConfig.teams.disable_teams
                && !world.getGameRules().getGameRuleBooleanValue(DISABLE_TEAM_CREATION);
    }

    static boolean canJoinTeam(World world) {
        return !ServerUtilitiesConfig.teams.disable_teams
                && !world.getGameRules().getGameRuleBooleanValue(DISABLE_TEAM_JOINING);
    }
}
