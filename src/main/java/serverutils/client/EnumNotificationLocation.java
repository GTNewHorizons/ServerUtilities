package serverutils.client;

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
}
