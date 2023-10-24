package serverutils.net;

import serverutils.lib.net.NetworkWrapper;

public class ServerUtilitiesNetHandler {

    static final NetworkWrapper GENERAL = NetworkWrapper.newWrapper("serverutilities");
    static final NetworkWrapper CLAIMS = NetworkWrapper.newWrapper("utilities_claim");
    static final NetworkWrapper FILES = NetworkWrapper.newWrapper("utilities_files");
    static final NetworkWrapper EDIT_CONFIG = NetworkWrapper.newWrapper("utilities_config");
    static final NetworkWrapper STATS = NetworkWrapper.newWrapper("utilities_stats");
    static final NetworkWrapper MY_TEAM = NetworkWrapper.newWrapper("utilities_my_team");

    public static void init() {
        GENERAL.register(new MessageSyncData());
        GENERAL.register(new MessageNotification());
        GENERAL.registerBlank();
        GENERAL.register(new MessageCloseGui());
        GENERAL.register(new MessageAdminPanelGui());
        GENERAL.register(new MessageAdminPanelGuiResponse());
        GENERAL.register(new MessageAdminPanelAction());
        GENERAL.register(new MessageRequestBadge());
        GENERAL.register(new MessageSendBadge());
        GENERAL.register(new MessageUpdateTabName());
        GENERAL.register(new MessageUpdatePlayTime());

        CLAIMS.register(new MessageClaimedChunksRequest());
        CLAIMS.register(new MessageClaimedChunksUpdate());
        CLAIMS.register(new MessageClaimedChunksModify());

        EDIT_CONFIG.register(new MessageEditConfig());
        EDIT_CONFIG.register(new MessageEditConfigResponse());

        FILES.register(new MessageEditNBT());
        FILES.register(new MessageEditNBTResponse());
        FILES.register(new MessageEditNBTRequest());
        FILES.register(new MessageViewCrashList());
        FILES.register(new MessageViewCrash());
        FILES.register(new MessageViewCrashResponse());
        FILES.register(new MessageViewCrashDelete());

        MY_TEAM.register(new MessageSelectTeamGui());
        MY_TEAM.register(new MessageMyTeamGui());
        MY_TEAM.register(new MessageMyTeamGuiResponse());
        MY_TEAM.register(new MessageMyTeamAction());
        MY_TEAM.register(new MessageMyTeamPlayerList());

        STATS.register(new MessageLeaderboardList());
        STATS.register(new MessageLeaderboardListResponse());
        STATS.register(new MessageLeaderboard());
        STATS.register(new MessageLeaderboardResponse());

    }
}
