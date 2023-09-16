package latmod.lib.net;

import java.util.HashMap;
import java.util.Map;

public class LinkBuilder {

    private final StringBuilder base;
    private HashMap<String, Object> args;

    public LinkBuilder(String init) {
        base = new StringBuilder(init);
    }

    public LinkBuilder append(String s) {
        base.append(s);
        return this;
    }

    public LinkBuilder put(String s, Object o) {
        if (args == null) args = new HashMap<>();
        args.put(s, o);
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(base);

        if (args != null && !args.isEmpty()) {
            boolean first = true;

            for (Map.Entry<String, Object> e : args.entrySet()) {
                sb.append(first ? '?' : '&');
                sb.append(e.getKey());
                sb.append('=');
                sb.append(e.getValue());
                first = false;
            }
        }

        return sb.toString();
    }
}
