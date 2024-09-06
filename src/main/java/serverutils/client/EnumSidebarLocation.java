package serverutils.client;

import net.minecraft.client.Minecraft;

public enum EnumSidebarLocation {

    DISABLED(true),
    TOP_LEFT(true),
    INVENTORY_SIDE(true),
    UNLOCKED(false);

    private final boolean locked;

    EnumSidebarLocation(boolean locked) {
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean above() {
        return this == INVENTORY_SIDE && !Minecraft.getMinecraft().thePlayer.getActivePotionEffects().isEmpty()
                && ServerUtilitiesClientConfig.sidebar_buttons_above_potion;
    }
}
