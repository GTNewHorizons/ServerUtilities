package serverutils;

import java.util.Comparator;

import net.minecraft.stats.StatList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.data.Leaderboard;
import serverutils.events.LeaderboardRegistryEvent;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.math.Ticks;

public final class ServerUtilitiesLeaderboards {

    private ServerUtilitiesLeaderboards() {}

    public static final ServerUtilitiesLeaderboards INST = new ServerUtilitiesLeaderboards();

    @SubscribeEvent
    @SuppressWarnings("unused") // used by reflection
    public void registerLeaderboards(LeaderboardRegistryEvent event) {
        event.register(
                new Leaderboard.FromStat(
                        new ResourceLocation(ServerUtilities.MOD_ID, "deaths"),
                        StatList.deathsStat,
                        false,
                        Leaderboard.FromStat.DEFAULT));
        event.register(
                new Leaderboard.FromStat(
                        new ResourceLocation(ServerUtilities.MOD_ID, "mob_kills"),
                        StatList.mobKillsStat,
                        false,
                        Leaderboard.FromStat.DEFAULT));
        event.register(
                new Leaderboard.FromStat(
                        new ResourceLocation(ServerUtilities.MOD_ID, "time_played"),
                        StatList.minutesPlayedStat,
                        false,
                        Leaderboard.FromStat.TIME));
        event.register(
                new Leaderboard.FromStat(
                        new ResourceLocation(ServerUtilities.MOD_ID, "time_afk"),
                        ServerUtilitiesStats.AFK_TIME,
                        false,
                        Leaderboard.FromStat.TIME));
        event.register(
                new Leaderboard.FromStat(
                        new ResourceLocation(ServerUtilities.MOD_ID, "jumps"),
                        StatList.jumpStat,
                        false,
                        Leaderboard.FromStat.DEFAULT));

        event.register(
                new Leaderboard(
                        new ResourceLocation(ServerUtilities.MOD_ID, "deaths_per_hour"),
                        new ChatComponentTranslation("serverutilities.stat.dph"),
                        player -> {
                            double d = getDPH(player);
                            return new ChatComponentText(d < 0D ? "-" : String.format("%.2f", d));
                        },
                        Comparator.comparingDouble(ServerUtilitiesLeaderboards::getDPH).reversed(),
                        player -> getDPH(player) >= 0D));

        event.register(
                new Leaderboard(
                        new ResourceLocation(ServerUtilities.MOD_ID, "time_active"),
                        new ChatComponentTranslation("serverutilities.stat.time_active"),
                        player -> Leaderboard.FromStat.TIME.apply(getActivePlayTime(player)),
                        Comparator.comparingLong(ServerUtilitiesLeaderboards::getActivePlayTime).reversed(),
                        player -> getActivePlayTime(player) != 0));
        event.register(
                new Leaderboard(
                        new ResourceLocation(ServerUtilities.MOD_ID, "time_afk_percent"),
                        new ChatComponentTranslation("serverutilities.stat.time_afk_percent"),
                        player -> Leaderboard.FromStat.PERCENTAGE.apply(getAfkTimeFraction(player)),
                        Comparator.comparingDouble(ServerUtilitiesLeaderboards::getAfkTimeFraction).reversed(),
                        player -> getAfkTimeFraction(player) != 0));
        event.register(
                new Leaderboard(
                        new ResourceLocation(ServerUtilities.MOD_ID, "last_seen"),
                        new ChatComponentTranslation("serverutilities.stat.last_seen"),
                        player -> {
                            if (player.isOnline()) {
                                IChatComponent component = new ChatComponentTranslation("gui.online");
                                component.getChatStyle().setColor(EnumChatFormatting.GREEN);
                                return component;
                            } else {
                                long worldTime = player.team.universe.world.getTotalWorldTime();
                                long time = worldTime - player.getLastTimeSeen();
                                return Leaderboard.FromStat.LONG_TIME.apply(time);
                            }
                        },
                        Comparator.comparingLong(ServerUtilitiesLeaderboards::getRelativeLastSeen),
                        player -> player.getLastTimeSeen() != 0L));
    }

    private static int getActivePlayTime(ForgePlayer player) {
        final int playTime = player.stats().writeStat(StatList.minutesPlayedStat);
        final int afkTime = player.stats().writeStat(ServerUtilitiesStats.AFK_TIME);
        return playTime - afkTime;
    }

    private static float getAfkTimeFraction(ForgePlayer player) {
        final float playTime = player.stats().writeStat(StatList.minutesPlayedStat);
        final float afkTime = player.stats().writeStat(ServerUtilitiesStats.AFK_TIME);
        return afkTime / Math.max(playTime, 1.0f);
    }

    private static long getRelativeLastSeen(ForgePlayer player) {
        if (player.isOnline()) {
            return 0;
        }

        return player.team.universe.ticks.ticks() - player.getLastTimeSeen();
    }

    private static double getDPH(ForgePlayer player) {
        int playTime = player.stats().writeStat(StatList.minutesPlayedStat);

        if (playTime > 0) {
            double hours = Ticks.get(playTime).hoursd();

            if (hours >= 1D) {
                return (double) player.stats().writeStat(StatList.deathsStat) / hours;
            }
        }

        return -1D;
    }
}
