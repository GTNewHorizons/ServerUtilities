package serverutils.lib.config;

import javax.annotation.Nullable;

public final class RankConfigValueInfo implements Comparable<RankConfigValueInfo> {

    public final String node;
    public final ConfigValue defaultValue;
    public final ConfigValue defaultOPValue;

    public RankConfigValueInfo(String s, ConfigValue def, @Nullable ConfigValue defOP) {
        node = s;
        defaultValue = def.copy();
        defaultOPValue = def.copy();

        if (defOP != null) {
            defaultOPValue.setValueFromOtherValue(defOP);
        }
    }

    public String toString() {
        return node;
    }

    public int hashCode() {
        return node.hashCode();
    }

    public boolean equals(Object o) {
        return o == this || o instanceof RankConfigValueInfo valueInfo && node.equals(valueInfo.node);
    }

    @Override
    public int compareTo(RankConfigValueInfo o) {
        return node.compareTo(o.node);
    }
}
