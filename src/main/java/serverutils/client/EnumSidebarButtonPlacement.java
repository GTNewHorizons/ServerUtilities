package serverutils.client;

import java.util.Locale;

import net.minecraft.client.Minecraft;

import serverutils.client.gui.SidebarButton;

public enum EnumSidebarButtonPlacement {

    DISABLED,
    TOP_LEFT,
    INVENTORY_SIDE,
    AUTO;

    public boolean top() {
        return switch (this) {
            case TOP_LEFT -> true;
            case AUTO -> SidebarButton.NEI_NOT_LOADED.getAsBoolean();
            default -> false;
        };
    }

    public boolean above() {
        return this == AUTO && !Minecraft.getMinecraft().thePlayer.getActivePotionEffects().isEmpty()
                && ServerUtilitiesClientConfig.sidebar_buttons_above_potion;
    }

    public static EnumSidebarButtonPlacement string2placement(String placement) {
        return switch (placement.toLowerCase(Locale.ENGLISH)) {
            case "disabled" -> DISABLED;
            case "top_left" -> TOP_LEFT;
            case "inventory_side" -> INVENTORY_SIDE;
            default -> AUTO;
        };
    }
}
