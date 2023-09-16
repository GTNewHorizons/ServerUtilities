package serverutils.utils.world.ranks;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.util.EnumChatFormatting;

import serverutils.lib.api.config.ConfigEntryString;
import serverutils.lib.api.config.ConfigFile;
import serverutils.lib.api.config.ConfigGroup;
import serverutils.utils.world.LMPlayerServer;

public class Ranks {

    public static final Rank PLAYER = new Rank("Player");
    public static final Rank ADMIN = new Rank("Admin");

    private static ConfigFile file;
    private static final ConfigEntryString default_rank = new ConfigEntryString("default_rank", "Player");
    private static final ConfigGroup ranks_group = new ConfigGroup("ranks");

    private static Rank defaultRank;
    private static final HashMap<String, Rank> ranks = new HashMap<>();
    private static final HashMap<UUID, Rank> playerMap = new HashMap<>();

    public static Rank getRankFor(LMPlayerServer p) {
        boolean enabled = false;

        if (enabled) {
            if (p == null || p.isFake()) return defaultRank;
            Rank r = playerMap.get(p.getProfile().getId());
            return (r == null) ? defaultRank : r;
        } else {
            if (p == null || p.isFake()) return PLAYER;
            return p.isOP() ? ADMIN : PLAYER;
        }
    }

    public static void load(ConfigFile file) {
        /*
         * file = new ConfigFile("ranks", new File(ServerUtilitiesLibrary.folderLocal, "serverutils/ranks.json"));
         * file.add(default_rank); file.add(ranks_group);
         */

        Ranks.ADMIN.setDefaults();
        Ranks.ADMIN.color.set(EnumChatFormatting.DARK_GREEN);

        Ranks.PLAYER.setDefaults();
        Ranks.PLAYER.color.set(EnumChatFormatting.WHITE);

        file.add(Ranks.ADMIN.config.getAsGroup("permissions_admin", false), false);
        file.add(Ranks.PLAYER.config.getAsGroup("permissions_player", false), false);

        reload();
    }

    public static void reload() {
        ranks.clear();
        playerMap.clear();

        /*
         * file.load(); if(ranks_group.entryMap.isEmpty()) { ConfigGroup def_player = new ConfigGroup("Player");
         * def_player.addAll(Rank.class, PLAYER, true); } saveRanks();
         */
    }

    public static void saveRanks() {
        // file.save();
    }

    public static Rank getRankFor(String s) {
        return ranks.get(s);
    }

    public static void setRank(Rank r) {
        // ranks.put(r.ID, r);
    }
}
