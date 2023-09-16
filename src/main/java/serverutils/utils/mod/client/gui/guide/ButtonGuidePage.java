package serverutils.utils.mod.client.gui.guide;

import java.util.List;

import net.minecraft.util.*;

import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.gui.widgets.ButtonLM;
import serverutils.utils.api.guide.GuidePage;

/**
 * Created by LatvianModder on 04.03.2016.
 */
public class ButtonGuidePage extends ButtonLM {

    public final GuiGuide guiGuide;
    public final GuidePage page;
    public String hover;

    public ButtonGuidePage(GuiGuide g, GuidePage p) {
        super(g, 0, g.panelPages.height, g.panelWidth - 36, 13);
        guiGuide = g;
        page = p;
        updateTitle();
    }

    public void onButtonPressed(int b) {
        ServerUtilitiesLibraryClient.playClickSound();

        if (page.childPages.isEmpty()) {
            guiGuide.selectedPage = page;
            guiGuide.sliderText.value = 0F;
            guiGuide.panelText.posY = 10;
            guiGuide.panelText.refreshWidgets();
        } else ServerUtilitiesLibraryClient.openGui(new GuiGuide(guiGuide, page));
    }

    public void updateTitle() {
        IChatComponent titleC = page.getTitleComponent().createCopy();
        if (guiGuide.selectedPage == page) titleC.getChatStyle().setBold(true);
        title = titleC.getFormattedText();
        hover = null;

        if (gui.getFontRenderer().getStringWidth(title) > width) {
            hover = title + "";
            title = gui.getFontRenderer().trimStringToWidth(title, width - 3) + "...";
        }
    }

    public void addMouseOverText(List<String> l) {
        if (hover != null) l.add(hover);
    }

    public void renderWidget() {
        int ay = getAY();
        if (ay < -height || ay > guiGuide.mainPanel.height) return;
        int ax = getAX();
        guiGuide.getFontRenderer().drawString(
                mouseOver(ax, ay) ? (EnumChatFormatting.UNDERLINE + title) : title,
                ax + 1,
                ay + 1,
                guiGuide.colorText);
    }
}
