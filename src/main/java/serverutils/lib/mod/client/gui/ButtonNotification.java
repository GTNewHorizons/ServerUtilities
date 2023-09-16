package serverutils.lib.mod.client.gui;

import java.util.List;

import net.minecraft.item.ItemStack;

import serverutils.lib.api.ServerUtilitiesLibLang;
import serverutils.lib.api.client.GlStateManager;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.gui.GuiIcons;
import serverutils.lib.api.gui.GuiLM;
import serverutils.lib.api.gui.widgets.ButtonLM;
import serverutils.lib.api.notification.ClientNotifications;

public class ButtonNotification extends ButtonLM {

    public final ClientNotifications.Perm notification;

    public ButtonNotification(GuiNotifications g, ClientNotifications.Perm n) {
        super(g, 0, 0, 0, 24);
        notification = n;
        posY += g.buttonList.size() * 25;
        title = n.notification.title.getFormattedText();
        width = gui.getFontRenderer().getStringWidth(n.notification.title.getFormattedText());
        if (n.notification.desc != null)
            width = Math.max(width, gui.getFontRenderer().getStringWidth(n.notification.desc.getFormattedText()));
        if (n.notification.item != null) width += 20;
        width += 8;
    }

    public void renderWidget() {
        int ax = getAX();
        int ay = getAY();

        int tx = 4;
        ItemStack is = notification.notification.item;
        if (is != null) {
            tx += 20;
            GuiLM.drawItem(gui, is, ax + 4, ay + 4);
        }

        GlStateManager.color(1F, 1F, 1F, 1F);

        ServerUtilitiesLibraryClient.setGLColor(notification.notification.color, mouseOver(ax, ay) ? 255 : 185);
        GuiLM.drawBlankRect(ax, ay, gui.getZLevel(), parentPanel.width, height);
        GlStateManager.color(1F, 1F, 1F, 1F);

        gui.getFontRenderer().drawString(title, ax + tx, ay + 4, 0xFFFFFFFF);
        if (notification.notification.desc != null) gui.getFontRenderer()
                .drawString(notification.notification.desc.getFormattedText(), ax + tx, ay + 14, 0xFFFFFFFF);

        if (mouseOver(ax, ay)) {
            float alpha = 0.4F;
            if (gui.mouse().x >= ax + width - 16) alpha = 1F;

            GlStateManager.color(1F, 1F, 1F, alpha);
            GuiLM.render(GuiIcons.close, ax + width - 18, ay + 4, gui.getZLevel());
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    public void onButtonPressed(int b) {
        ServerUtilitiesLibraryClient.playClickSound();

        if (gui.mouse().x < getAX() + width - 16) notification.onClicked();
        ClientNotifications.Perm.remove(notification);

        gui.initLMGui();
        gui.refreshWidgets();
    }

    public void addMouseOverText(List<String> l) {
        int ax = getAX();
        if (mouseOver(ax, getAY()) && gui.mouse().x >= ax + width - 16) {
            l.add(ServerUtilitiesLibLang.button_close.format());
            return;
        }

        if (notification.notification.mouse != null) notification.notification.mouse.addHoverText(l);
    }
}
