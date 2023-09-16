package ftb.lib.api.notification;

import java.util.HashMap;

import latmod.lib.LMListUtils;

public class ClickActionRegistry {

    private static final HashMap<String, ClickActionType> map = new HashMap<>();

    static {
        add(ClickActionType.CMD);
        add(ClickActionType.SHOW_CMD);
        add(ClickActionType.URL);
        add(ClickActionType.FILE);
        add(ClickActionType.GUI);
    }

    public static String[] getKeys() {
        return LMListUtils.toStringArray(map.keySet());
    }

    public static void add(ClickActionType a) {
        if (a != null && !map.containsKey(a.getID())) map.put(a.getID(), a);
    }

    public static ClickActionType get(String s) {
        return map.get(s);
    }
}
