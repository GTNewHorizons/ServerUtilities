package serverutils.aurora.mc;

import net.minecraft.server.MinecraftServer;

import serverutils.aurora.AuroraConfig;
import serverutils.aurora.PageType;
import serverutils.aurora.page.HTTPWebPage;
import serverutils.aurora.tag.Style;
import serverutils.aurora.tag.Tag;
import serverutils.lib.command.CommandUtils;
import serverutils.ranks.ICommandWithPermission;

public class CommandListPage extends HTTPWebPage {

    private final MinecraftServer server;

    public CommandListPage(MinecraftServer s) {
        server = s;
    }

    @Override
    public String getTitle() {
        return "Server Utilities";
    }

    @Override
    public String getDescription() {
        return "Command List";
    }

    @Override
    public String getIcon() {
        return "https:i.imgur.com/aIuCGYZ.png";
    }

    @Override
    public PageType getPageType() {
        return AuroraConfig.pages.command_list_page;
    }

    @Override
    public void head(Tag head) {
        super.head(head);
        Style s = head.style();
        s.add("p").set("margin", "0");
    }

    @Override
    public void body(Tag body) {
        body.h1("Command List");

        Tag nodeTable = body.table();
        Tag firstRow = nodeTable.tr();
        firstRow.th().text("Available command nodes");
        firstRow.th().text("Usage");

        for (ICommandWithPermission cmd : CommandUtils.getPermissionCommands()) {
            Tag row = nodeTable.tr();
            row.td().paired("code", cmd.serverutilities$getPermissionNode());
        }
    }
}
