package serverutils.lib.mod.client.gui;

import java.util.ArrayList;
import java.util.Collections;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.MathHelperLM;
import serverutils.lib.api.client.GlStateManager;
import serverutils.lib.api.gui.GuiLM;
import serverutils.lib.api.notification.ClientNotifications;
import serverutils.lib.mod.client.ServerUtilitiesLibraryModClient;

@SideOnly(Side.CLIENT)
public class GuiNotifications extends GuiLM {

    public final ArrayList<ButtonNotification> buttonList;

    public GuiNotifications(GuiScreen parent) {
        super(parent, null);
        mainPanel.height = 25 * 7;

        buttonList = new ArrayList<>();
    }

    public void initLMGui() {
        mainPanel.width = 0;

        buttonList.clear();

        Collections.sort(ClientNotifications.Perm.list, null);

        int s = Math.min(ClientNotifications.Perm.list.size(), 7);

        for (int i = 0; i < s; i++) {
            ClientNotifications.Perm p = ClientNotifications.Perm.list.get(i);
            ButtonNotification b = new ButtonNotification(this, p);
            buttonList.add(b);
            mainPanel.width = Math.max(mainPanel.width, b.width);
        }

        mainPanel.width = MathHelperLM.clampInt(mainPanel.width, 200, 300);

        for (ButtonNotification b : buttonList) b.width = mainPanel.width;
    }

    public void addWidgets() {
        mainPanel.addAll(buttonList);
    }

    public void drawBackground() {
        super.drawBackground();

        fontRendererObj.drawString(
                I18n.format(ServerUtilitiesLibraryModClient.notifications.getFullID()),
                mainPanel.posX + 4,
                mainPanel.posY - 11,
                0xFFFFFFFF);

        GlStateManager.color(0F, 0F, 0F, 0.4F);
        drawBlankRect(mainPanel.posX, mainPanel.posY, zLevel, mainPanel.width, mainPanel.height);

        for (int i = 1; i < 7; i++)
            drawBlankRect(mainPanel.posX, mainPanel.posY + i * 25 - 1, zLevel, mainPanel.width, 1);

        GlStateManager.color(1F, 1F, 1F, 1F);

        for (ButtonNotification b : buttonList) b.renderWidget();
    }
}
