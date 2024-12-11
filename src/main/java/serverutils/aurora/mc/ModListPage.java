package serverutils.aurora.mc;

import java.util.Set;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import serverutils.aurora.AuroraConfig;
import serverutils.aurora.PageType;
import serverutils.aurora.page.HTTPWebPage;
import serverutils.aurora.tag.Style;
import serverutils.aurora.tag.Tag;

public class ModListPage extends HTTPWebPage {

    private final Set<String> excludedMods;

    public ModListPage(Set<String> set) {
        excludedMods = set;
    }

    @Override
    public String getTitle() {
        return "Minecraft";
    }

    @Override
    public String getDescription() {
        return "Mod List";
    }

    @Override
    public PageType getPageType() {
        return AuroraConfig.pages.modlist_page;
    }

    @Override
    public void head(Tag head) {
        super.head(head);
        Style s = head.style();
        s.add("span.num").set("margin-right", "0.8em");
    }

    @Override
    public void body(Tag body) {
        Tag table = body.table();
        Tag row = table.tr();
        row.th().text("Mod List");
        row.th().text("Version");

        int i = 0;

        for (ModContainer container : Loader.instance().getModList()) {
            if (!excludedMods.contains(container.getModId())) {
                row = table.tr();
                Tag t = row.td();
                t.span(String.valueOf(++i), "num");
                t.a(container.getName(), "/modlist/" + container.getModId());
                row.td().text(container.getDisplayVersion());
            }
        }
    }
}
