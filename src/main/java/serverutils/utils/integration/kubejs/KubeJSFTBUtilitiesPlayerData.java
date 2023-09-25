package serverutils.utils.integration.kubejs;

// import com.feed_the_beast.ftblib.lib.data.Universe;
// import serverutils.utils.ServerUtilitiesConfig;
// import serverutils.utils.data.ClaimedChunk;
// import serverutils.utils.data.ClaimedChunks;
// import serverutils.utils.data.FTBUtilitiesPlayerData;
// import serverutils.utils.data.FTBUtilitiesTeamData;
// import serverutils.utils.ranks.Rank;
// import serverutils.utils.ranks.Ranks;
// import dev.latvian.kubejs.player.PlayerDataJS;
//
// import javax.annotation.Nullable;
// import java.util.Collections;
// import java.util.OptionalInt;
// import java.util.Set;

/**
 * @author LatvianModder
 */
public class KubeJSFTBUtilitiesPlayerData {
    // private final PlayerDataJS playerData;
    // private FTBUtilitiesPlayerData cached;
    //
    // public KubeJSFTBUtilitiesPlayerData(PlayerDataJS d)
    // {
    // playerData = d;
    // }
    //
    // public FTBUtilitiesPlayerData getWrappedPlayerData()
    // {
    // if (cached == null)
    // {
    // cached = FTBUtilitiesPlayerData.get(Universe.get().getPlayer(playerData.getPlayerEntity()));
    // }
    //
    // return cached;
    // }
    //
    // @Nullable
    // public Rank getRank()
    // {
    // return Ranks.isActive() ? Ranks.INSTANCE.getPlayerRank(playerData.getProfile()) : null;
    // }
    //
    // public Set<ClaimedChunk> getClaimedChunks()
    // {
    // if (ClaimedChunks.isActive())
    // {
    // return ClaimedChunks.instance.getTeamChunks(getWrappedPlayerData().player.team, OptionalInt.empty());
    // }
    //
    // return Collections.emptySet();
    // }
    //
    // public boolean getAfk()
    // {
    // return getWrappedPlayerData().afkTime >= ServerUtilitiesConfig.afk.getNotificationTimer();
    // }
    //
    // public boolean getEnablePVP()
    // {
    // return getWrappedPlayerData().enablePVP();
    // }
    //
    // public String getNickname()
    // {
    // return getWrappedPlayerData().getNickname();
    // }
    //
    // public void setNickname(String nickname)
    // {
    // getWrappedPlayerData().setNickname(nickname);
    // }
    //
    // public int getMaxClaimChunks()
    // {
    // return FTBUtilitiesTeamData.get(getWrappedPlayerData().player.team).getMaxClaimChunks();
    // }
    //
    // public int getMaxChunkloaderChunks()
    // {
    // return FTBUtilitiesTeamData.get(getWrappedPlayerData().player.team).getMaxChunkloaderChunks();
    // }
}
