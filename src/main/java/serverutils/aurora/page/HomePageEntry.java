package serverutils.aurora.page;

import java.util.ArrayList;
import java.util.List;

public class HomePageEntry implements Comparable<HomePageEntry> {

    public final String title;
    public final String url;
    public String icon;
    public final List<HomePageEntry> entries;

    public HomePageEntry(String t, String u) {
        title = t;
        url = u;
        icon = "";
        entries = new ArrayList<>();
    }

    public HomePageEntry(String t, String u, String i) {
        this(t, u);
        icon = i;
    }

    public HomePageEntry add(HomePageEntry entry) {
        entries.add(entry);
        return this;
    }

    @Override
    public int compareTo(HomePageEntry o) {
        return title.compareToIgnoreCase(o.title);
    }
}
