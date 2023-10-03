package serverutils.utils;

import java.util.Comparator;

import net.minecraft.stats.StatList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.math.Ticks;
import serverutils.mod.ServerUtilities;
import serverutils.utils.data.Leaderboard;
import serverutils.utils.events.LeaderboardRegistryEvent;

public class ServerUtilitiesLeaderboards {

    public static final ServerUtilitiesLeaderboards INST = new ServerUtilitiesLeaderboards();

    @SubscribeEvent
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
                        new ResourceLocation(ServerUtilities.MOD_ID, "last_seen"),
                        new ChatComponentTranslation("serverutilities.stat.last_seen"),
                        player -> {
                            if (player.isOnline()) {
                                IChatComponent component = new ChatComponentTranslation("gui.online");
                                component.getChatStyle().setColor(EnumChatFormatting.GREEN);
                                return component;
                            } else {
                                long worldTime = player.team.universe.world.getTotalWorldTime();
                                int time = (int) (worldTime - player.getLastTimeSeen());
                                return Leaderboard.FromStat.TIME.apply(time);
                            }
                        },
                        Comparator.comparingLong(ServerUtilitiesLeaderboards::getRelativeLastSeen),
                        player -> player.getLastTimeSeen() != 0L));
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
                return (double) player.stats().writeStat(StatList.damageDealtStat) / hours;
            }
        }

        return -1D;
    }
}
