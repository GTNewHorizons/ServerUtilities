package serverutils.client;

import net.minecraft.client.gui.ScaledResolution;

import com.gtnewhorizons.dashboard.api.HudElement;
import com.gtnewhorizons.dashboard.api.HudRegistry;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Moves and hides the notifications with GTNHLib's text above the hotbar in Dashboard's HUD editor, since they show at
 * almost the same spot. The only class using Dashboard, so only call it while Dashboard is loaded.
 */
@SideOnly(Side.CLIENT)
public final class DashboardHud {

    private static final String ABOVE_HOTBAR_TEXT = "gtnhlib:above_hotbar_text";

    private static boolean moved;

    private DashboardHud() {}

    /**
     * False means the player hid the text, so skip drawing the notification. With an older GTNHLib, which does not have
     * this element yet, the notification is drawn at its usual spot.
     */
    public static boolean beginNotification(ScaledResolution resolution) {
        HudElement text = HudRegistry.getElement(ABOVE_HOTBAR_TEXT);
        if (text == null) {
            return true;
        }
        moved = HudRegistry.beginRender(text, resolution);
        return moved;
    }

    public static void endNotification() {
        if (moved) {
            moved = false;
            HudRegistry.endRender();
        }
    }
}
