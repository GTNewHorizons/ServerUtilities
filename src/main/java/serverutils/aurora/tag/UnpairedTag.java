package serverutils.aurora.tag;

import java.util.Map;

public class UnpairedTag extends Tag {

    public UnpairedTag(String n) {
        super(n);
    }

    @Override
    public void build(StringBuilder builder) {
        builder.append('<');
        builder.append(name);

        if (attributes != null && !attributes.isEmpty()) {
            for (Map.Entry<String, String> map : attributes.entrySet()) {
                builder.append(' ');
                builder.append(map.getKey());
                builder.append('=');
                builder.append('"');
                builder.append(map.getValue());
                builder.append('"');
            }
        }

        builder.append('>');
    }

    @Override
    public Tag tooltip() {
        int index = parent.children.indexOf(this);
        PairedTag div = new PairedTag("div");
        div.parent = parent;
        parent.children.set(index, div);
        div.addClass("tooltip");
        div.append(this);
        Tag div2 = div.paired("div");
        div2.addClass("tooltiptext");
        return div2;
    }
}
