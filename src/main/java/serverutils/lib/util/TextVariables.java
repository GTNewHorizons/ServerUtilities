package serverutils.lib.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;

import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;

/**
 * The {@code {variables}} of the MOTD and of the tab list header and footer. Names are case-sensitive; text in curly
 * brackets that is not a variable stays as it is. The wiki page "Variables" lists them for server owners.
 */
public final class TextVariables {

    private static final Pattern VARIABLE = Pattern.compile("\\{([A-Za-z]+)}");
    private static final DecimalFormat ONE_DECIMAL = new DecimalFormat("0.0");
    private static final long MEGABYTE = 1024L * 1024L;

    /** About the server as a whole; the same for everyone. */
    private static final Map<String, Function<MinecraftServer, String>> SERVER = new HashMap<>();
    /** About the player the text is shown to. */
    private static final Map<String, Function<EntityPlayerMP, String>> PLAYER = new HashMap<>();

    private static long serverStartTime;

    static {
        SERVER.put("players", server -> String.valueOf(server.getCurrentPlayerCount()));
        SERVER.put("maxPlayers", server -> String.valueOf(server.getMaxPlayers()));
        SERVER.put(
                "uniquePlayers",
                server -> Universe.loaded() ? String.valueOf(Universe.get().getPlayers().size()) : "0");
        SERVER.put("tps", server -> ONE_DECIMAL.format(tps(server)));
        SERVER.put("tpsColor", server -> tps(server) >= 19.0 ? "\u00a7a" : tps(server) >= 15.0 ? "\u00a7e" : "\u00a7c");
        SERVER.put("mspt", server -> ONE_DECIMAL.format(mspt(server)));
        SERVER.put(
                "msptColor",
                server -> mspt(server) <= 40.0 ? "\u00a7a" : mspt(server) <= 50.0 ? "\u00a7e" : "\u00a7c");
        SERVER.put("memory", server -> usedMemory() + "/" + maxMemory() + "MB");
        SERVER.put("memoryUsed", server -> String.valueOf(usedMemory()));
        SERVER.put("memoryMax", server -> String.valueOf(maxMemory()));
        SERVER.put("memoryPercent", server -> String.valueOf(usedMemory() * 100L / Math.max(1L, maxMemory())));
        SERVER.put("uptime", server -> formatDuration(System.currentTimeMillis() - startTime()));
        SERVER.put("entities", server -> String.valueOf(count(server, world -> world.loadedEntityList.size())));
        SERVER.put("tileEntities", server -> String.valueOf(count(server, world -> world.loadedTileEntityList.size())));
        SERVER.put(
                "chunks",
                server -> String.valueOf(count(server, world -> world.theChunkProviderServer.getLoadedChunkCount())));
        SERVER.put("dimensions", server -> String.valueOf(count(server, world -> 1)));
        SERVER.put("time", server -> new SimpleDateFormat("HH:mm").format(new Date()));
        SERVER.put("date", server -> new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        SERVER.put("day", server -> String.valueOf(overworldTime(server) / 24000L + 1L));
        SERVER.put("worldTime", server -> formatWorldTime(overworldTime(server)));

        PLAYER.put("player", EntityPlayerMP::getCommandSenderName);
        PLAYER.put("ping", player -> String.valueOf(player.ping));
        PLAYER.put("pingColor", player -> pingColor(player.ping));
        PLAYER.put("dimension", player -> ServerUtils.getDimensionName(player.dimension).getUnformattedText());
        PLAYER.put("x", player -> String.valueOf(MathHelper.floor_double(player.posX)));
        PLAYER.put("y", player -> String.valueOf(MathHelper.floor_double(player.posY)));
        PLAYER.put("z", player -> String.valueOf(MathHelper.floor_double(player.posZ)));
        PLAYER.put(
                "biome",
                player -> player.worldObj.getBiomeGenForCoords(
                        MathHelper.floor_double(player.posX),
                        MathHelper.floor_double(player.posZ)).biomeName);
        PLAYER.put("health", player -> String.valueOf(MathHelper.ceiling_float_int(player.getHealth())));
        PLAYER.put("food", player -> String.valueOf(player.getFoodStats().getFoodLevel()));
        PLAYER.put("level", player -> String.valueOf(player.experienceLevel));
        PLAYER.put("gamemode", player -> player.theItemInWorldManager.getGameType().getName());
        PLAYER.put("team", TextVariables::teamTitle);
        PLAYER.put(
                "playtime",
                player -> formatDuration(player.func_147099_x().writeStat(StatList.minutesPlayedStat) * 50L));
        PLAYER.put("deaths", player -> String.valueOf(player.func_147099_x().writeStat(StatList.deathsStat)));
    }

    private TextVariables() {}

    /** Uptime counts from here. */
    public static void serverStarted() {
        serverStartTime = System.currentTimeMillis();
    }

    /**
     * @param player who the text is for; without one the variables about a player stay as they are
     */
    public static String replace(String text, MinecraftServer server, @Nullable EntityPlayerMP player) {
        return replace(text, name -> {
            Function<MinecraftServer, String> ofServer = SERVER.get(name);
            if (ofServer != null) return ofServer.apply(server);
            Function<EntityPlayerMP, String> ofPlayer = player == null ? null : PLAYER.get(name);
            return ofPlayer == null ? null : ofPlayer.apply(player);
        });
    }

    /** @param values the text for a variable name, or null for a name that is not a variable */
    static String replace(String text, Function<String, String> values) {
        if (text.indexOf('{') < 0) return text;
        Matcher matcher = VARIABLE.matcher(text);
        StringBuffer result = new StringBuffer(text.length() + 16);
        while (matcher.find()) {
            String value = values.apply(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? matcher.group() : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static boolean hasVariables(String text) {
        return contains(text, SERVER) || contains(text, PLAYER);
    }

    public static boolean hasPlayerVariables(String text) {
        return contains(text, PLAYER);
    }

    private static boolean contains(String text, Map<String, ?> variables) {
        if (text.indexOf('{') < 0) return false;
        Matcher matcher = VARIABLE.matcher(text);
        while (matcher.find()) {
            if (variables.containsKey(matcher.group(1))) return true;
        }
        return false;
    }

    private static double mspt(MinecraftServer server) {
        return MathHelper.average(server.tickTimeArray) * 1.0E-6D;
    }

    private static double tps(MinecraftServer server) {
        return Math.min(20.0, 1000.0 / mspt(server));
    }

    private static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / MEGABYTE;
    }

    private static long maxMemory() {
        return Runtime.getRuntime().maxMemory() / MEGABYTE;
    }

    private static long startTime() {
        if (serverStartTime == 0L) serverStarted();
        return serverStartTime;
    }

    private static long count(MinecraftServer server, Function<WorldServer, Integer> perWorld) {
        long total = 0L;
        for (WorldServer world : server.worldServers) {
            if (world != null) total += perWorld.apply(world);
        }
        return total;
    }

    private static long overworldTime(MinecraftServer server) {
        WorldServer overworld = server.worldServerForDimension(0);
        return overworld == null ? 0L : overworld.getWorldTime();
    }

    /** The clock of the world: tick 0 of a day is 06:00. */
    static String formatWorldTime(long worldTime) {
        long ticks = ((worldTime + 6000L) % 24000L + 24000L) % 24000L;
        return String.format("%02d:%02d", ticks / 1000L, ticks % 1000L * 60L / 1000L);
    }

    static String formatDuration(long millis) {
        long seconds = millis / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        long days = hours / 24L;

        if (days > 0L) {
            return days + "d " + (hours % 24L) + "h";
        } else if (hours > 0L) {
            return hours + "h " + (minutes % 60L) + "m";
        } else if (minutes > 0L) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }

    /** The colours of the ping legend in the default tab list footer. */
    static String pingColor(int ping) {
        if (ping < 0) return "\u00a78";
        if (ping < 150) return "\u00a7a";
        if (ping < 300) return "\u00a7e";
        if (ping < 600) return "\u00a7c";
        if (ping < 1000) return "\u00a74";
        return "\u00a78";
    }

    private static String teamTitle(EntityPlayerMP player) {
        if (!Universe.loaded() || ServerUtils.isFake(player)) return "";
        ForgePlayer forgePlayer = Universe.get().getPlayer(player);
        return forgePlayer == null || !forgePlayer.hasTeam() ? "" : forgePlayer.team.getTitle().getUnformattedText();
    }
}
