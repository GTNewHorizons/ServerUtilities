package serverutils.aurora.mc;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import serverutils.aurora.AuroraConfig;
import serverutils.aurora.PageType;
import serverutils.aurora.page.HTTPWebPage;
import serverutils.aurora.tag.Tag;

public class PlayerListTable extends HTTPWebPage {

    private final MinecraftServer server;

    public PlayerListTable(MinecraftServer s) {
        server = s;
    }

    @Override
    public String getTitle() {
        return "Minecraft";
    }

    @Override
    public String getDescription() {
        return "Online Players";
    }

    @Override
    public PageType getPageType() {
        switch (AuroraConfig.general.player_list_table) {
            case "DISABLED":
                return PageType.DISABLED;
            case "REQUIRES_AUTH":
                return PageType.REQUIRES_AUTH;
            default:
                return PageType.ENABLED;
        }
    }

    @Override
    public String getStylesheet() {
        return "";
    }

    @Override
    public boolean addBackButton() {
        return false;
    }

    @Override
    public void body(Tag body) {
        Tag playerTable = body.table().addClass("player_table");

        for (EntityPlayerMP player : (List<EntityPlayerMP>) server.getConfigurationManager().playerEntityList) {
            String id = player.getUniqueID().toString().replace("-", "");
            Tag row = playerTable.tr();
            row.td().img("https://crafatar.com/avatars/" + id + "?size=16");
            row.td().a(player.getDisplayName(), "https://mcuuid.net/?q=" + id);
        }
    }
}
