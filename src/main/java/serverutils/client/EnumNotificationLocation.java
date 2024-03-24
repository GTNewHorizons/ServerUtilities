package serverutils.client;

import java.util.Locale;

public enum EnumNotificationLocation {

    DISABLED,
    CHAT,
    SCREEN;

    public boolean chat() {
        return this == CHAT;
    }

    public boolean disabled() {
        return this == DISABLED;
    }

    public static EnumNotificationLocation stringToEnum(String placement) {
        return switch (placement.toLowerCase(Locale.ENGLISH)) {
            case "disabled" -> DISABLED;
            case "chat" -> CHAT;
            default -> SCREEN;
        };
    }
}
