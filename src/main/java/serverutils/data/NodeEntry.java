package serverutils.data;

import javax.annotation.Nullable;

import serverutils.lib.config.ConfigBoolean;
import serverutils.lib.config.ConfigValue;
import serverutils.lib.util.permission.DefaultPermissionLevel;

public final class NodeEntry implements Comparable<NodeEntry> {

    public final String node;
    public final ConfigValue player;
    public final ConfigValue op;
    public final String desc;
    public final DefaultPermissionLevel level;

    public NodeEntry(String n, ConfigValue p, ConfigValue o, String d, @Nullable DefaultPermissionLevel l) {
        node = n;
        player = p;
        op = o;
        desc = d;
        level = l;
    }

    public NodeEntry(String n, DefaultPermissionLevel l, String d) {
        this(
                n,
                new ConfigBoolean(l == DefaultPermissionLevel.ALL),
                new ConfigBoolean(l != DefaultPermissionLevel.NONE),
                d,
                l);
    }

    public String getNode() {
        return node;
    }

    public String toString() {
        return node;
    }

    public int hashCode() {
        return node.hashCode();
    }

    public boolean equals(Object o) {
        return o == this || o instanceof NodeEntry entry && node.equals(entry.node);
    }

    @Override
    public int compareTo(NodeEntry o) {
        return node.compareTo(o.node);
    }
}
