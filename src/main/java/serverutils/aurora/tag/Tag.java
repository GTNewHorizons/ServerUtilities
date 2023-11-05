package serverutils.aurora.tag;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.EnumChatFormatting;

public abstract class Tag extends TagBase {

    public static String fixHTML(String string) {
        return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public final String name;
    protected Map<String, String> attributes;

    public Tag(String n) {
        name = n;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String getAttribute(String key) {
        return attributes == null || attributes.isEmpty() ? "" : attributes.getOrDefault(key, "");
    }

    public Tag attr(String key, String value) {
        if (attributes == null) {
            attributes = new LinkedHashMap<>();
        }

        attributes.put(key, value);
        return this;
    }

    public Tag title(String title) {
        return attr("title", title);
    }

    public Tag title(Iterable<String> title) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (String t : title) {
            String s = EnumChatFormatting.getTextWithoutFormattingCodes(t);

            if (first) {
                first = false;
            } else {
                sb.append('\n');
            }

            sb.append(s);
            sb.append(' ');
        }

        return title(sb.toString());
    }

    public Style style() {
        Style style = new Style();
        append(style);
        return style;
    }

    public Tag style(String key, String value) {
        attr("style", getAttribute("style") + key + ':' + value + ';');
        return this;
    }

    public Tag id(String id) {
        return attr("id", id);
    }

    public Tag meta(String name, String content) {
        return unpaired("meta").attr("name", name).attr("content", content);
    }

    public Tag addClass(String c) {
        if (c.isEmpty()) {
            return this;
        }

        String s = getAttribute("class");
        attr("class", s.isEmpty() ? c : (s + " " + c));
        return this;
    }

    public <T extends TagBase> T append(T child) {
        return child;
    }

    public Tag text(Object txt) {
        String text = String.valueOf(txt);

        if (!text.isEmpty()) {
            append(new TextTag(text));
        }

        return this;
    }

    public Tag paired(String tag, String text) {
        return append(new PairedTag(tag, text));
    }

    public Tag paired(String tag) {
        return paired(tag, "");
    }

    public Tag unpaired(String tag) {
        return append(new UnpairedTag(tag));
    }

    public Tag h1(String text) {
        return paired("h1", text);
    }

    public Tag h2(String text) {
        return paired("h2", text);
    }

    public Tag h3(String text) {
        return paired("h3", text);
    }

    public Tag p(String text) {
        return paired("p", text);
    }

    public Tag p() {
        return p("");
    }

    public Tag a(String text, String url) {
        return paired("a", text).attr("href", url);
    }

    public Tag img(String img) {
        return unpaired("img").attr("src", img);
    }

    public Tag span(String text) {
        return paired("span", text);
    }

    public Tag span(String text, String c) {
        return span(text).addClass(c);
    }

    public Tag br() {
        return unpaired("br");
    }

    public Tag ul() {
        return paired("ul");
    }

    public Tag ol() {
        return paired("ol");
    }

    public Tag li() {
        return paired("li");
    }

    public Tag table() {
        return paired("table");
    }

    public Tag tr() {
        return paired("tr");
    }

    public Tag th() {
        return paired("th");
    }

    public Tag td() {
        return paired("td");
    }

    public Tag tooltip() {
        addClass("tooltip");
        Tag div = paired("div");
        div.addClass("tooltiptext");
        return div;
    }

    public Tag tooltip(String text) {
        tooltip().text(text);
        return this;
    }

    public Tag icon(String name) {
        return img("https://aurora.latvian.dev/icons/" + name + ".png").addClass("icon"); // TODO: Change this to a
                                                                                          // local image
    }

    public Tag yesNoSpan(boolean value) {
        return span(value ? "Yes" : "No", value ? "yes" : "no");
    }
}
