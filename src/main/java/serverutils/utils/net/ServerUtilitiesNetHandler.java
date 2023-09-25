package serverutils.utils.net;

import serverutils.lib.lib.net.NetworkWrapper;

public class ServerUtilitiesNetHandler {

    static final NetworkWrapper GENERAL = NetworkWrapper.newWrapper("serverutilities");
    static final NetworkWrapper CLAIMS = NetworkWrapper.newWrapper("serverutilities_claims");
    static final NetworkWrapper FILES = NetworkWrapper.newWrapper("serverutilities_files");
    static final NetworkWrapper STATS = NetworkWrapper.newWrapper("serverutilities_stats");

    public static void init() {
        GENERAL.register(new MessageRequestBadge());
        GENERAL.register(new MessageSendBadge());
        GENERAL.register(new MessageUpdateTabName());
        GENERAL.register(new MessageUpdatePlayTime());

        CLAIMS.register(new MessageClaimedChunksRequest());
        CLAIMS.register(new MessageClaimedChunksUpdate());
        CLAIMS.register(new MessageClaimedChunksModify());

        FILES.register(new MessageEditNBT());
        FILES.register(new MessageEditNBTResponse());
        FILES.register(new MessageEditNBTRequest());
        FILES.register(new MessageViewCrashList());
        FILES.register(new MessageViewCrash());
        FILES.register(new MessageViewCrashResponse());
        FILES.register(new MessageViewCrashDelete());

        STATS.register(new MessageLeaderboardList());
        STATS.register(new MessageLeaderboardListResponse());
        STATS.register(new MessageLeaderboard());
        STATS.register(new MessageLeaderboardResponse());
    }
}
