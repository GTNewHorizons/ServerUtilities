package serverutils.utils.integration.aurora;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.aurora.AuroraHomePageEvent;
import serverutils.aurora.AuroraPageEvent;
import serverutils.aurora.page.HomePageEntry;

public class AuroraIntegration {

    public static final AuroraIntegration INST = new AuroraIntegration();

    @SubscribeEvent
    public void onAuroraHomePageEvent(AuroraHomePageEvent event) {
        event.add(
                new HomePageEntry("Server Utilities", "server-utilities", "https://i.imgur.com/SDV8WV5.png").add(
                        new HomePageEntry("Ranks", "ranks", "https://i.imgur.com/3o2sHns.png").add(
                                new HomePageEntry("Permission List", "permissions", "https://i.imgur.com/m8KTq4s.png"))
                                .add(
                                        new HomePageEntry(
                                                "Command List",
                                                "commands",
                                                "https://i.imgur.com/aIuCGYZ.png"))));
    }

    @SubscribeEvent
    public void onAuroraEvent(AuroraPageEvent event) {
        if (event.checkPath("server-utilities", "ranks", "permissions")) {
            event.returnPage(new PermissionListPage());
        } else if (event.checkPath("server-utilities", "ranks", "commands")) {
            event.returnPage(new CommandListPage(event.getAuroraServer().getServer()));
        } else if (event.checkPath("server-utilities", "ranks")) {
            event.returnPage(new RankPage());
        }
    }
}
