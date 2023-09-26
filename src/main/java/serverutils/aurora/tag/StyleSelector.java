package serverutils.aurora.tag;

import java.util.LinkedHashMap;
import java.util.Map;

public class StyleSelector extends TagBase {

    public final String selector;
    public final Map<String, String> properties;

    public StyleSelector(String s) {
        selector = s;
        properties = new LinkedHashMap<>();
    }

    public StyleSelector set(String key, String value) {
        properties.put(key, value);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public void build(StringBuilder builder) {
        builder.append(selector);
        builder.append('{');
        int i = 0;

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            builder.append(entry.getKey());
            builder.append(':');
            builder.append(entry.getValue());

            if (++i < properties.size()) {
                builder.append(';');
            }
        }

        builder.append('}');
    }
}
