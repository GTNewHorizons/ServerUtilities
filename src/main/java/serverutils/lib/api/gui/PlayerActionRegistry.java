package serverutils.lib.api.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import serverutils.lib.api.PlayerAction;
import serverutils.lib.api.config.ConfigEntryBool;
import serverutils.lib.api.config.ConfigGroup;
import serverutils.lib.api.friends.ILMPlayer;

/**
 * Created by LatvianModder on 15.01.2016.
 */
public class PlayerActionRegistry {

    private static final HashMap<String, PlayerAction> map = new HashMap<>();

    public static final ConfigGroup configGroup = new ConfigGroup("sidebar_buttons");

    public static boolean enabled(String id) {
        return !configGroup.entryMap.containsKey(id) || configGroup.getEntry(id).getAsBoolean();
    }

    public static void add(final PlayerAction a) {
        if (a != null) {
            map.put(a.getID(), a);

            if (a.configDefault() != null) {
                ConfigEntryBool entry = new ConfigEntryBool(a.getID(), a.configDefault().booleanValue()) {

                    public String getFullID() {
                        return "player_action." + a.getID();
                    }
                };

                configGroup.add(entry, false);
            }
        }
    }

    public static List<PlayerAction> getPlayerActions(PlayerAction.Type t, ILMPlayer self, ILMPlayer other,
            boolean sort, boolean ignoreConfig) {
        ArrayList<PlayerAction> l = new ArrayList<>();

        for (PlayerAction a : map.values()) {
            if (a.type.equalsType(t) && a.isVisibleFor(self, other)) {
                if (!ignoreConfig && a.configDefault() != null) {
                    if (!enabled(a.getID())) continue;
                }

                l.add(a);
            }
        }

        if (sort) Collections.sort(l);
        return l;
    }
}
