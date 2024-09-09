package serverutils;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import serverutils.data.Leaderboard;
import serverutils.events.LeaderboardRegistryEvent;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.io.DataReader;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.JsonUtils;
import serverutils.lib.util.permission.DefaultPermissionLevel;
import serverutils.lib.util.permission.PermissionAPI;

public final class ServerUtilitiesLeaderboards {

    public static final Map<ResourceLocation, Leaderboard> LEADERBOARDS = new HashMap<>();
    private static final File STAT_LEADERBOARD_FILE = new File(
            ServerUtilities.SERVER_FOLDER + "stat_leaderboards.json");
    private static final String[] DEFAULT_STAT_LEADERBOARDS = { StatList.deathsStat.statId,
            StatList.mobKillsStat.statId, StatList.minutesPlayedStat.statId, ServerUtilitiesStats.AFK_TIME.statId,
            StatList.jumpStat.statId };

    static void loadLeaderboards() {
        LEADERBOARDS.clear();
        registerLeaderboard(
                new Leaderboard(
                        new ResourceLocation(ServerUtilities.MOD_ID, "deaths_per_hour"),
                        new ChatComponentTranslation("serverutilities.stat.dph"),
                        player -> {
                            double d = getDPH(player);
                            return new ChatComponentText(d < 0D ? "-" : String.format("%.2f", d));
                        },
                        Comparator.comparingDouble(ServerUtilitiesLeaderboards::getDPH).reversed(),
                        player -> getDPH(player) >= 0D));
        registerLeaderboard(
                new Leaderboard(
                        new ResourceLocation(ServerUtilities.MOD_ID, "time_active"),
                        new ChatComponentTranslation("serverutilities.stat.time_active"),
                        player -> Leaderboard.FromStat.TIME.apply(getActivePlayTime(player)),
                        Comparator.comparingLong(ServerUtilitiesLeaderboards::getActivePlayTime).reversed(),
                        player -> getActivePlayTime(player) != 0));
        registerLeaderboard(
                new Leaderboard(
                        new ResourceLocation(ServerUtilities.MOD_ID, "time_afk_percent"),
                        new ChatComponentTranslation("serverutilities.stat.time_afk_percent"),
                        player -> Leaderboard.FromStat.PERCENTAGE.apply(getAfkTimeFraction(player)),
                        Comparator.comparingDouble(ServerUtilitiesLeaderboards::getAfkTimeFraction).reversed(),
                        player -> getAfkTimeFraction(player) != 0));
        registerLeaderboard(
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
        loadFromFile();
        new LeaderboardRegistryEvent(ServerUtilitiesLeaderboards::registerLeaderboard).post();
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

    private static JsonElement getAndSaveDefaults() {
        JsonObject obj = new JsonObject();
        for (String id : DEFAULT_STAT_LEADERBOARDS) {
            JsonObject obj1 = new JsonObject();
            obj1.addProperty("name", "");
            obj1.addProperty("reverse", false);
            obj.add(id, obj1);
        }

        JsonObject example = new JsonObject();
        example.addProperty("name", "The name that appears in the gui, leave empty/remove for default");
        example.addProperty(
                "reverse",
                "(true || false) whether lower numbers should appear highest in the leaderboard");
        obj.add("example.Stat || Full list of usable stats can be dumped with /dump_stats", example);
        JsonUtils.toJsonSafe(STAT_LEADERBOARD_FILE, obj);
        return obj;
    }

    private static void loadFromFile() {
        JsonElement element;
        if (!STAT_LEADERBOARD_FILE.exists()) {
            element = getAndSaveDefaults();
        } else {
            element = DataReader.get(STAT_LEADERBOARD_FILE).safeJson();
        }

        if (JsonUtils.isNull(element)) return;

        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("example")) continue;

                StatBase stat = StatList.func_151177_a(key);
                if (stat == null) {
                    ServerUtilities.LOGGER.warn("Couldn't find stat with id {}, skipping", key);
                    continue;
                }

                JsonElement value = entry.getValue();
                if (value.isJsonObject()) {
                    JsonObject obj = value.getAsJsonObject();

                    IChatComponent name;
                    JsonElement nameElem = obj.getAsJsonPrimitive("name");
                    if (nameElem != null && !nameElem.getAsString().isEmpty()) {
                        name = new ChatComponentText(nameElem.getAsString());
                    } else {
                        name = getSafeName(stat);
                    }

                    boolean reverse = false;
                    JsonElement reverseElem = obj.getAsJsonPrimitive("reverse");
                    if (reverseElem != null && reverseElem.getAsBoolean()) {
                        reverse = true;
                    }

                    registerLeaderboard(
                            new Leaderboard.FromStat(
                                    new ResourceLocation(ServerUtilities.MOD_ID, key),
                                    name,
                                    stat,
                                    reverse,
                                    getStatIntFunction(stat)));

                }
            }
        }
    }

    private static IntFunction<IChatComponent> getStatIntFunction(StatBase stat) {
        if (stat.type.equals(StatBase.timeStatType)) {
            return Leaderboard.FromStat.TIME;
        }

        return Leaderboard.FromStat.DEFAULT;
    }

    private static IChatComponent getSafeName(StatBase stat) {
        IChatComponent component = stat.statName;
        String id = stat.statId;
        if (component instanceof ChatComponentTranslation translation && translation.getFormatArgs().length > 0) {
            if (translation.getFormatArgs()[0] instanceof ChatComponentTranslation arg) {
                if (id.startsWith("stat.entityKilledBy")) {
                    return new ChatComponentTranslation("serverutilities.stat.killed_by", arg.getUnformattedText());
                } else if (id.startsWith("stat.killEntity")) {
                    return new ChatComponentTranslation(
                            "serverutilities.stat.entities_killed",
                            arg.getUnformattedText());
                }
            }
        }
        return component;
    }

    public static void registerLeaderboard(Leaderboard leaderboard) {
        LEADERBOARDS.put(leaderboard.id, leaderboard);
        PermissionAPI.registerNode(
                ServerUtilitiesPermissions.getLeaderboardNode(leaderboard),
                DefaultPermissionLevel.ALL,
                "");
    }
}
