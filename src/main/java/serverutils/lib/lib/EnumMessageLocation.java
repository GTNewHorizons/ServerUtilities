package serverutils.lib.lib;

import serverutils.lib.lib.util.misc.NameMap;

public enum EnumMessageLocation {

    OFF("options.narrator.off"),
    CHAT("options.chat.visibility"),
    ACTION_BAR("action_bar");

    public static final NameMap<EnumMessageLocation> NAME_MAP = NameMap
            .createWithTranslation(CHAT, (sender, value) -> value.translationKey, values());

    public final String translationKey;

    EnumMessageLocation(String k) {
        translationKey = k;
    }
}
