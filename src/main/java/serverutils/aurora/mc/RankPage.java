package serverutils.aurora.mc;

import serverutils.aurora.AuroraConfig;
import serverutils.aurora.PageType;
import serverutils.aurora.page.HTTPWebPage;
import serverutils.aurora.tag.Style;
import serverutils.aurora.tag.Tag;
import serverutils.ranks.Rank;
import serverutils.ranks.Ranks;

public class RankPage extends HTTPWebPage {

    public RankPage() {}

    @Override
    public String getTitle() {
        return "Server Utilities";
    }

    @Override
    public String getDescription() {
        return "Ranks";
    }

    @Override
    public String getIcon() {
        return "https:i.imgur.com/3o2sHns.png";
    }

    @Override
    public PageType getPageType() {
        return AuroraConfig.pages.player_rank_page;
    }

    @Override
    public void head(Tag head) {
        super.head(head);
        Style s = head.style();
        s.add("p").set("margin", "0");
        s.add("th").set("font-weight", "normal");
    }

    @Override
    public void body(Tag body) {
        body.h1("Ranks");

        for (Rank rank : Ranks.INSTANCE.ranks.values()) {
            Tag table = body.table();
            Tag name = table.tr().th().attr("colspan", "2");
            name.span(rank.getId(), "other");

            for (Rank.Entry entry : rank.permissions.values()) {
                Tag row = table.tr();
                row.td().text(entry.node);

                if (entry.value.equals("true") || entry.value.equals("false")) {
                    row.td().span(entry.value, entry.value.equals("true") ? "yes" : "no");
                } else {
                    row.td().span(entry.value, "other");
                }
            }

            body.br();
        }
    }
}
