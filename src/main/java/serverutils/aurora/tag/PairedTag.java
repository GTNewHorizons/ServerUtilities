package serverutils.aurora.tag;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PairedTag extends Tag {

    public List<TagBase> children;

    public PairedTag(String n, String t) {
        super(n);

        if (!t.isEmpty()) {
            append(new TextTag(t));
        }
    }

    public PairedTag(String n) {
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

        if (children != null && !children.isEmpty()) {
            for (TagBase child : children) {
                child.build(builder);
            }
        }

        builder.append('<');
        builder.append('/');
        builder.append(name);
        builder.append('>');
    }

    @Override
    public <T extends TagBase> T append(T child) {
        if (children == null) {
            children = new LinkedList<>();
        }

        children.add(child);
        child.parent = this;
        return child;
    }
}
