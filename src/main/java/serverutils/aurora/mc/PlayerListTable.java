package serverutils.aurora.mc;

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
        return AuroraConfig.pages.player_list_table;
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

        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            String id = player.getUniqueID().toString().replace("-", "");
            Tag row = playerTable.tr();
            row.td().img("https://crafatar.com/avatars/" + id + "?size=16");
            row.td().a(player.getDisplayName(), "https://mcuuid.net/?q=" + id);
        }
    }
}
