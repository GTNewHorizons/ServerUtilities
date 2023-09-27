package serverutils.utils.integration.aurora;

import net.minecraft.server.MinecraftServer;

import serverutils.aurora.AuroraConfig;
import serverutils.aurora.PageType;
import serverutils.aurora.page.HTTPWebPage;
import serverutils.aurora.tag.Style;
import serverutils.aurora.tag.Tag;
import serverutils.utils.ranks.CommandOverride;
import serverutils.utils.ranks.Ranks;

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
        switch (AuroraConfig.general.permission_list_page) {
            case "DISABLED":
                return PageType.DISABLED;
            case "REQUIRES_AUTH":
                return PageType.REQUIRES_AUTH;
            default:
                return PageType.ENABLED;
        }
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

        for (CommandOverride c : Ranks.INSTANCE.commands.values()) {
            Tag row = nodeTable.tr();
            row.td().paired("code", c.node.toString());
            Tag n = row.td();
            boolean first = true;

            for (String s : Tag.fixHTML(c.usage.getUnformattedText()).split(" OR ")) {
                if (first) {
                    first = false;
                } else {
                    n.br();
                }

                n.text(s);
            }
        }
    }
}
