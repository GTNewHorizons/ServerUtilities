package ftb.lib.api.notification;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ftb.lib.api.client.FTBLibClient;
import ftb.lib.api.client.GlStateManager;
import ftb.lib.api.gui.GuiLM;
import latmod.lib.LMUtils;
import latmod.lib.util.FinalIDObject;

@SideOnly(Side.CLIENT)
public class ClientNotifications {

    private static Temp current = null;

    public static void renderTemp() {
        if (current != null) {
            if (current.render()) current = null;
        } else if (!Temp.list.isEmpty()) {
            current = Temp.list.get(0);
            Temp.list.remove(0);
        }
    }

    public static void add(Notification n) {
        if (n == null) return;
        Temp.list.remove(n);
        Perm.list.remove(n);
        if (current != null && current.equals(n)) current = null;
        Temp.list.add(new Temp(n));
        if (!n.isTemp()) Perm.list.add(new Perm(n));
    }

    public static void init() {
        current = null;
        Perm.list.clear();
        Temp.list.clear();
    }

    public static class Temp extends FinalIDObject {

        public static final List<Temp> list = new ArrayList<>();

        private long time;
        private Notification notification;
        private String title;
        private String desc;
        private int width;

        private Temp(Notification n) {
            super(n.getID());
            n.item = null; // Too lazy to re-implement item drawing
            notification = n;
            time = -1L;
            title = notification.title.getFormattedText();
            desc = (notification.desc == null) ? null : notification.desc.getFormattedText();
            width = 20 + Math.max(
                    FTBLibClient.mc.fontRenderer.getStringWidth(title),
                    FTBLibClient.mc.fontRenderer.getStringWidth(desc));
            if (notification.item != null) width += 20;
        }

        public boolean render() {
            if (time == -1L) time = Minecraft.getSystemTime();

            if (time > 0L) {
                double d0 = (double) (Minecraft.getSystemTime() - time) / (double) notification.timer;

                if (d0 < 0D || d0 > 1D) {
                    time = 0L;
                    return true;
                }

                double d1 = d0 * 2D;

                if (d1 > 1D) d1 = 2D - d1;
                d1 *= 4D;
                d1 = 1D - d1;

                if (d1 < 0D) d1 = 0D;

                d1 *= d1;
                d1 *= d1;

                GlStateManager.disableDepth();
                GlStateManager.pushMatrix();
                GlStateManager.depthMask(false);
                GlStateManager.translate(FTBLibClient.displayW - width, -d1 * 36D, 0F);

                GlStateManager.disableTexture2D();
                GlStateManager.disableLighting();
                FTBLibClient.setGLColor(notification.color, 230);
                GuiLM.drawBlankRect(0D, 0D, 0D, FTBLibClient.displayW, 32D);
                GlStateManager.enableTexture2D();
                GlStateManager.color(1F, 1F, 1F, 1F);

                int w = notification.item == null ? 10 : 30;

                FontRenderer font = FTBLibClient.mc.fontRenderer;

                if (desc == null) {
                    font.drawString(title, w, 12, 0xFFFFFFFF);
                } else {
                    font.drawString(title, w, 7, 0xFFFFFFFF);
                    font.drawString(desc, w, 18, 0xFFFFFFFF);
                }

                if (notification.item != null) {
                    // FTBLibClient.renderGuiItem(notification.item, FTBLibClient.mc.getRenderItem(), font, 8, 8);
                }

                GlStateManager.depthMask(true);
                GlStateManager.color(1F, 1F, 1F, 1F);
                GlStateManager.popMatrix();
                GlStateManager.enableLighting();
            }

            return time == 0L;
        }
    }

    public static class Perm extends FinalIDObject {

        public static final List<Perm> list = new ArrayList<>();

        public static void remove(Object o) {
            int index = list.indexOf(o);
            if (index != -1) list.remove(index);
        }

        public final Notification notification;
        public final long timeAdded;

        private Perm(Notification n) {
            super(n.getID());
            notification = n;
            timeAdded = LMUtils.millis();
        }

        public int compareTo(Object o) {
            return Long.compare(((Perm) o).timeAdded, timeAdded);
        }

        public void onClicked() {
            if (notification.mouse != null && notification.mouse.click != null) notification.mouse.click.onClicked();
        }
    }
}
