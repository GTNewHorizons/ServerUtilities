package serverutils.data;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.config.ConfigCategory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.ServerUtilitiesPermissions;
import serverutils.lib.EnumTeamColor;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.TeamType;
import serverutils.lib.data.Universe;
import serverutils.lib.math.BlockDimPos;
import serverutils.lib.math.ChunkDimPos;
import serverutils.lib.util.JsonUtils;
import serverutils.lib.util.StringUtils;
import serverutils.ranks.Rank;

public class BackwardsCompat {

    public static JsonObject LATCONFIG = JsonUtils.fromJson(Universe.get().server.getFile("local/ftbu/config.json"))
            .getAsJsonObject();

    // Only runs on first load
    public static void load() {
        loadPlayers();
        loadChunks();
        loadWarps();
        loadConfig();
    }

    public static void loadPlayers() {
        NBTTagCompound tagPlayers = readMap(new File(Universe.get().latModFolder, "LMPlayers.dat"));
        ServerUtilities.LOGGER.info("Loading players from LatMod");

        if (tagPlayers != null && tagPlayers.hasKey("Players")) {
            NBTTagCompound pTag = tagPlayers.getCompoundTag("Players");
            Map<String, NBTTagCompound> map = toMapWithType(pTag);
            for (Map.Entry<String, NBTTagCompound> e : map.entrySet()) {
                NBTTagCompound tag1 = e.getValue();
                UUID uuid = StringUtils.fromString(tag1.getString("UUID"));
                if (uuid != null) {
                    ForgePlayer player = new ForgePlayer(Universe.get(), uuid, tag1.getString("Name"));
                    Universe.get().players.put(uuid, player);

                    player.lastTimeSeen = tag1.getCompoundTag("Stats").getLong("LastSeen");
                    // Load player homes from Latmod
                    // Ignores home limit
                    ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(player);
                    NBTTagCompound homes = tag1.getCompoundTag("Homes");
                    for (String s1 : getMapKeys(homes)) {
                        data.homes.set(s1, BlockDimPos.fromIntArray(homes.getIntArray(s1)));
                    }
                    data.player.markDirty();
                }
            }
        }
    }

    public static void loadChunks() {
        // Loads the chunks from the ClaimedChunks.json file
        // Ignores claim/chunk load limit
        JsonObject group = JsonUtils.fromJson(new File(Universe.get().latModFolder, "ClaimedChunks.json"))
                .getAsJsonObject();
        if (group == null) return;

        for (Map.Entry<String, JsonElement> e : group.entrySet()) {
            int dim = Integer.parseInt(e.getKey());
            Universe universe = Universe.get();
            ServerUtilities.LOGGER.info("Loading claimed chunks from LatMod");

            for (Map.Entry<String, JsonElement> e1 : e.getValue().getAsJsonObject().entrySet()) {
                try {
                    ForgePlayer p = Universe.get().getPlayer(StringUtils.fromString(e1.getKey()));

                    if (p != null) {
                        // If player exists in the ClaimedChunks.json file, create a team for them
                        if (p.team.type == TeamType.NONE) {
                            ForgeTeam team = new ForgeTeam(
                                    universe,
                                    universe.generateTeamUID((short) 0),
                                    p.getName(),
                                    TeamType.PLAYER);
                            team.owner = p;
                            team.setColor(EnumTeamColor.NAME_MAP.getRandom(universe.world.rand));
                            universe.addTeam(team);
                            p.team = team;
                            p.markDirty();
                        }
                        ServerUtilitiesTeamData data = ServerUtilitiesTeamData.get(p.team);
                        JsonArray chunksList = e1.getValue().getAsJsonArray();

                        for (int k = 0; k < chunksList.size(); k++) {
                            int[] ai = fromIntArray(chunksList.get(k));

                            if (ai != null) {
                                ClaimedChunk c = new ClaimedChunk(
                                        new ChunkDimPos(new ChunkCoordIntPair(ai[0], ai[1]), dim),
                                        data);

                                if (ai.length >= 3 && ai[2] == 1) c.setLoaded(true);
                                ClaimedChunks.instance.addChunk(c);
                            }
                        }
                        p.team.markDirty();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static void loadWarps() {
        JsonObject warps = JsonUtils.fromJson(new File(Universe.get().latModFolder, "LMWorld.json")).getAsJsonObject()
                .get("warps").getAsJsonObject();
        ServerUtilities.LOGGER.info("Loading warps from LatMod");

        if (warps != null) for (Map.Entry<String, JsonElement> e : warps.entrySet()) {
            if (e.getValue().isJsonArray()) {
                int[] val = fromIntArray(e.getValue());
                ServerUtilitiesUniverseData.WARPS.set(e.getKey(), BlockDimPos.fromIntArray(val));
            } else {
                JsonObject o = e.getValue().getAsJsonObject();
                ServerUtilitiesUniverseData.WARPS.set(
                        e.getKey(),
                        new BlockDimPos(
                                o.get("x").getAsInt(),
                                o.get("y").getAsInt(),
                                o.get("z").getAsInt(),
                                o.get("dim").getAsInt()));
            }
        }
    }

    public static void loadConfig() {
        if (LATCONFIG == null) return;

        JsonObject latBackup = LATCONFIG.get("backups").getAsJsonObject();
        ConfigCategory backupConfig = ServerUtilitiesConfig.config.getCategory("backups");
        backupConfig.get("enable_backups").set(latBackup.get("enabled").getAsBoolean());
        backupConfig.get("backup_timer").set(latBackup.get("backup_timer").getAsDouble());
        backupConfig.get("backups_to_keep").set(latBackup.get("backups_to_keep").getAsInt());
        backupConfig.get("backup_folder_path").set(latBackup.get("folder").getAsString());
        backupConfig.get("use_separate_thread").set(latBackup.get("use_separate_thread").getAsBoolean());
        backupConfig.get("need_online_players").set(latBackup.get("need_online_players").getAsBoolean());
        backupConfig.get("compression_level").set(latBackup.get("compression_level").getAsInt());
        backupConfig.get("display_file_size").set(latBackup.get("display_file_size").getAsBoolean());

        JsonObject latGeneral = LATCONFIG.get("general").getAsJsonObject();
        ConfigCategory worldConfig = ServerUtilitiesConfig.config.getCategory("world");
        worldConfig.get("safe_spawn").set(latGeneral.get("safe_spawn").getAsBoolean());
        worldConfig.get("spawn_area_in_sp").set(latGeneral.get("spawn_area_in_sp").getAsBoolean());
        worldConfig.get("chunk_loading")
                .set(LATCONFIG.get("chunkloading").getAsJsonObject().get("enabled").getAsBoolean());

        ServerUtilitiesConfig.sync();
    }

    public static void loadRanks(Rank player, Rank admin) {
        if (LATCONFIG == null) return;

        JsonObject permissionsPlayer = LATCONFIG.get("permissions_player").getAsJsonObject();
        player.setPermission(ServerUtilitiesPermissions.HOMES_MAX, permissionsPlayer.get("max_homes").getAsInt());
        player.setPermission(
                ServerUtilitiesPermissions.CLAIMS_MAX_CHUNKS,
                permissionsPlayer.get("max_claims").getAsInt());
        player.setPermission(
                ServerUtilitiesPermissions.CHUNKLOADER_MAX_CHUNKS,
                permissionsPlayer.get("max_loaded_chunks").getAsInt());
        if (permissionsPlayer.get("cross_dim_homes").getAsBoolean()) {
            player.setPermission(ServerUtilitiesPermissions.HOMES_CROSS_DIM, true);
        }

        JsonObject permissionsAdmin = LATCONFIG.get("permissions_admin").getAsJsonObject();
        admin.setPermission(ServerUtilitiesPermissions.HOMES_MAX, permissionsAdmin.get("max_homes").getAsInt());
        admin.setPermission(
                ServerUtilitiesPermissions.CLAIMS_MAX_CHUNKS,
                permissionsAdmin.get("max_claims").getAsInt());
        admin.setPermission(
                ServerUtilitiesPermissions.CHUNKLOADER_MAX_CHUNKS,
                permissionsAdmin.get("max_loaded_chunks").getAsInt());
        if (permissionsAdmin.get("cross_dim_homes").getAsBoolean()) {
            admin.setPermission(ServerUtilitiesPermissions.HOMES_CROSS_DIM, true);
        }
    }

    public static <E extends NBTBase> Map<String, E> toMapWithType(NBTTagCompound tag) {
        HashMap<String, E> map = new HashMap<>();
        if (tag.hasNoTags()) return map;
        for (Object s : tag.func_150296_c()) map.put(s.toString(), (E) tag.getTag(s.toString()));
        return map;
    }

    public static NBTTagCompound readMap(File f) {
        if (!f.exists()) return null;
        try {
            return CompressedStreamTools.read(f);
        } catch (Exception e) {
            e.printStackTrace();
            ServerUtilities.LOGGER.info("Possibly corrupted / old file. Trying the old method");

            try {
                FileInputStream is = new FileInputStream(f);
                byte[] b = new byte[is.available()];
                is.read(b);
                is.close();
                return CompressedStreamTools.func_152457_a(b, NBTSizeTracker.field_152451_a);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    public static String[] getMapKeys(NBTTagCompound tag) {
        if (tag.hasNoTags()) return new String[0];
        return toStringArray(tag.func_150296_c());
    }

    public static String[] toStringArray(Collection<?> c) {
        if (c.isEmpty()) return null;
        String[] s = new String[c.size()];
        int i = -1;
        for (Object o : c) s[++i] = String.valueOf(o);
        return s;
    }

    public static int[] fromIntArray(JsonElement e) {
        if (e.isJsonNull()) return null;
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            int[] ai = new int[a.size()];
            if (ai.length == 0) return ai;
            for (int i = 0; i < ai.length; i++) ai[i] = a.get(i).getAsInt();
            return ai;
        }

        return new int[] { e.getAsInt() };
    }
}
