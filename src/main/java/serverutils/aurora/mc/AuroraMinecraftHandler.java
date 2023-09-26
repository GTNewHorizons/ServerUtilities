package serverutils.aurora.mc;

import java.util.Arrays;
import java.util.HashSet;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.aurora.AuroraConfig;
import serverutils.aurora.AuroraHomePageEvent;
import serverutils.aurora.AuroraPageEvent;
import serverutils.aurora.page.HomePageEntry;

public class AuroraMinecraftHandler {

    @SubscribeEvent
    public static void onHomeEvent(AuroraHomePageEvent event) {
        event.add(new HomePageEntry("Mod List", "modlist", "https://i.imgur.com/yQNrfg7.png"));
        event.add(
                new HomePageEntry("Minecraft", "minecraft", "https://i.imgur.com/1aRpzK0.png")
                        .add(new HomePageEntry("Online Players", "online_players", "https://i.imgur.com/a5dkvFu.png"))
                        .add(
                                new HomePageEntry(
                                        "Online Players API",
                                        "online_players.json",
                                        "https://i.imgur.com/a5dkvFu.png"))
                        .add(
                                new HomePageEntry(
                                        "World Info API",
                                        "world_info.json",
                                        "https://i.imgur.com/OVxZy1w.png")));
    }

    @SubscribeEvent
    public static void onPageEvent(AuroraPageEvent event) {
        if (event.checkPath("modlist", "*")) {
            HashSet<String> set = new HashSet<>(Arrays.asList(AuroraConfig.modlist_excluded_mods));

            if (!set.contains(event.getSplitUri()[1])) {
                ModContainer modContainer = Loader.instance().getIndexedModList().get(event.getSplitUri()[1]);

                if (modContainer != null) {
                    event.returnPage(new ModPage(modContainer));
                }
            }
        } else if (event.checkPath("modlist")) {
            event.returnPage(new ModListPage(new HashSet<>(Arrays.asList(AuroraConfig.modlist_excluded_mods))));
        } else if (event.checkPath("minecraft", "online_players")) {
            event.returnPage(new PlayerListTable(event.getAuroraServer().getServer()));
        } else if (event.checkPath("minecraft", "online_players.json")) {
            event.returnPage(new PlayerListJson(event.getAuroraServer().getServer()));
        } else if (event.checkPath("minecraft", "world_info.json")) {
            event.returnPage(new WorldInfoJson(event.getAuroraServer().getServer()));
        }
    }
}
