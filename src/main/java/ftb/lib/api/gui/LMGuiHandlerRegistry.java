package ftb.lib.api.gui;

import java.util.HashMap;

public class LMGuiHandlerRegistry {

    private static final HashMap<String, LMGuiHandler> map = new HashMap<>();

    public static void add(LMGuiHandler h) {
        if (h != null && !map.containsKey(h.ID)) map.put(h.ID, h);
    }

    public static LMGuiHandler get(String id) {
        return map.get(id);
    }
}
