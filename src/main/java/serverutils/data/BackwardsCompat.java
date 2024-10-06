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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

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

    public static JsonObject LATCONFIG;
    static {
        File ftbuFile = Universe.get().server.getFile("local/ftbu/config.json");
        if (ftbuFile.exists()) {
            LATCONFIG = JsonUtils.fromJson(ftbuFile).getAsJsonObject();
        }
    }

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
        ServerUtilities.LOGGER.info("Finished loading players from LatMod");
    }

    public static void loadChunks() {
        // Loads the chunks from the ClaimedChunks.json file
        // Ignores claim/chunk load limit
        Universe universe = Universe.get();
        JsonObject group = JsonUtils.fromJson(new File(universe.latModFolder, "ClaimedChunks.json")).getAsJsonObject();
        if (group == null) return;
        ServerUtilities.LOGGER.info("Loading claimed chunks from LatMod");
        if (!ClaimedChunks.isActive()) {
            ClaimedChunks.instance = new ClaimedChunks(universe);
        }
        for (Map.Entry<String, JsonElement> e : group.entrySet()) {
            int dim = Integer.parseInt(e.getKey());
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
        ServerUtilities.LOGGER.info("Finished loading claimed chunks from LatMod");
        ClaimedChunks.instance.forceSave();
    }

    public static void loadWarps() {
        JsonObject warps = JsonUtils.fromJson(new File(Universe.get().latModFolder, "LMWorld.json")).getAsJsonObject()
                .get("warps").getAsJsonObject();
        ServerUtilities.LOGGER.info("Loading warps from LatMod");

        if (warps != null) for (Map.Entry<String, JsonElement> e : warps.entrySet()) {
            if (e.getValue().isJsonArray()) {
                int[] val = fromIntArray(e.getValue());
                ServerUtilitiesUniverseData.WARPS.set(e.getKey().toLowerCase(), BlockDimPos.fromIntArray(val));
            } else {
                JsonObject o = e.getValue().getAsJsonObject();
                ServerUtilitiesUniverseData.WARPS.set(
                        e.getKey().toLowerCase(),
                        new BlockDimPos(
                                o.get("x").getAsInt(),
                                o.get("y").getAsInt(),
                                o.get("z").getAsInt(),
                                o.get("dim").getAsInt()));
            }
        }
        ServerUtilities.LOGGER.info("Finished loading warps from LatMod");
    }

    public static void loadConfig() {
        if (LATCONFIG == null) return;
        ServerUtilities.LOGGER.info("Loading config from LatMod");
        JsonObject latBackup = LATCONFIG.get("backups").getAsJsonObject();
        ServerUtilitiesConfig.backups.enable_backups = latBackup.get("enabled").getAsBoolean();
        ServerUtilitiesConfig.backups.backup_timer = latBackup.get("backup_timer").getAsDouble();
        ServerUtilitiesConfig.backups.backups_to_keep = latBackup.get("backups_to_keep").getAsInt();
        ServerUtilitiesConfig.backups.backup_folder_path = latBackup.get("folder").getAsString();
        ServerUtilitiesConfig.backups.use_separate_thread = latBackup.get("use_separate_thread").getAsBoolean();
        ServerUtilitiesConfig.backups.need_online_players = latBackup.get("need_online_players").getAsBoolean();
        ServerUtilitiesConfig.backups.compression_level = latBackup.get("compression_level").getAsInt();
        ServerUtilitiesConfig.backups.display_file_size = latBackup.get("display_file_size").getAsBoolean();

        JsonObject latGeneral = LATCONFIG.get("general").getAsJsonObject();
        ServerUtilitiesConfig.world.safe_spawn = latGeneral.get("safe_spawn").getAsBoolean();
        ServerUtilitiesConfig.world.spawn_area_in_sp = latGeneral.get("spawn_area_in_sp").getAsBoolean();
        ServerUtilitiesConfig.world.chunk_loading = LATCONFIG.get("chunkloading").getAsJsonObject().get("enabled")
                .getAsBoolean();

        ConfigurationManager.save(ServerUtilitiesConfig.class);
        ServerUtilities.LOGGER.info("Finished loading configs from LatMod");
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
        if (permissionsPlayer.get("cross_dim_warp").getAsBoolean()) {
            player.setPermission(ServerUtilitiesPermissions.WARPS_CROSS_DIM, true);
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
        if (permissionsAdmin.get("cross_dim_warp").getAsBoolean()) {
            admin.setPermission(ServerUtilitiesPermissions.WARPS_CROSS_DIM, true);
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
