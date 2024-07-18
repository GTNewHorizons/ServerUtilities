package serverutils;

import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatBasic;
import net.minecraft.util.ChatComponentTranslation;

public final class ServerUtilitiesStats {

    private ServerUtilitiesStats() {}

    public static final StatBase AFK_TIME = (new StatBasic(
            "serverutilities.stat.time_afk",
            new ChatComponentTranslation("serverutilities.stat.time_afk"),
            StatBase.timeStatType)).initIndependentStat().registerStat();

    /** Ensures the class is loaded and all stats are registered */
    public static void init() {
        // no-op
    }
}
