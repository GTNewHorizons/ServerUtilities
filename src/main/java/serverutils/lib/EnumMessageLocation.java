package serverutils.lib;

import serverutils.lib.util.misc.NameMap;

public enum EnumMessageLocation {

    OFF("options.off"),
    CHAT("options.chat.visibility"),
    ACTION_BAR("action_bar"),
    TITLE("title");

    public static final NameMap<EnumMessageLocation> NAME_MAP = NameMap
            .createWithTranslation(CHAT, (sender, value) -> value.translationKey, values());

    public final String translationKey;

    EnumMessageLocation(String k) {
        translationKey = k;
    }
}
