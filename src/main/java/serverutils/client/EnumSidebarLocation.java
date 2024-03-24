package serverutils.client;

import java.util.Locale;

import net.minecraft.client.Minecraft;

public enum EnumSidebarLocation {

    DISABLED("disabled", true),
    TOP_LEFT("top_left", true),
    INVENTORY_SIDE("inventory_side", true),
    UNLOCKED("unlocked", false);

    private final String location;
    private final boolean locked;

    EnumSidebarLocation(String location, boolean locked) {
        this.location = location.toLowerCase();
        this.locked = locked;
    }

    public String getLocation() {
        return location;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean above() {
        return this == INVENTORY_SIDE && !Minecraft.getMinecraft().thePlayer.getActivePotionEffects().isEmpty()
                && ServerUtilitiesClientConfig.sidebar_buttons_above_potion;
    }

    public static EnumSidebarLocation stringToEnum(String placement) {
        return switch (placement.toLowerCase(Locale.ENGLISH)) {
            case "disabled" -> DISABLED;
            case "top_left" -> TOP_LEFT;
            case "inventory_side" -> INVENTORY_SIDE;
            default -> UNLOCKED;
        };
    }
}
