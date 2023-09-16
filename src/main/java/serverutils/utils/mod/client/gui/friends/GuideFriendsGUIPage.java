package serverutils.utils.mod.client.gui.friends;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.PlayerAction;
import serverutils.lib.api.client.GlStateManager;
import serverutils.lib.api.gui.GuiLM;
import serverutils.lib.api.gui.PlayerActionRegistry;
import serverutils.utils.api.guide.GuidePage;
import serverutils.utils.mod.client.gui.guide.ButtonGuidePage;
import serverutils.utils.mod.client.gui.guide.GuiGuide;
import serverutils.utils.net.ClientAction;
import serverutils.utils.world.LMPlayerClient;
import serverutils.utils.world.LMWorldClient;

/**
 * Created by LatvianModder on 24.03.2016.
 */
@SideOnly(Side.CLIENT)
public class GuideFriendsGUIPage extends GuidePage {

    public final LMPlayerClient playerLM;

    public GuideFriendsGUIPage(LMPlayerClient p) {
        super(p.getProfile().getName());
        playerLM = p;
    }

    public void onClientDataChanged() {
        clear();

        text.add(new GuidePlayerViewLine(this, playerLM));

        if (!playerLM.clientInfo.isEmpty()) {
            for (String s : playerLM.clientInfo) printlnText(s);

            text.add(null);
        }

        for (PlayerAction a : PlayerActionRegistry
                .getPlayerActions(PlayerAction.Type.OTHER, LMWorldClient.inst.clientPlayer, playerLM, true, true)) {
            text.add(new GuidePlayerActionLine(this, playerLM, a));
        }

        /*
         * if(LMWorldClient.inst.clientPlayer.isFriend(playerLM)) { text.add(null); text.add(new
         * GuidePlayerInventoryLine(this, playerLM)); }
         */
    }

    public ButtonGuidePage createButton(GuiGuide gui) {
        return new Button(gui, this);
    }

    private class Button extends ButtonGuidePage {

        public Button(GuiGuide g, GuideFriendsGUIPage p) {
            super(g, p);
            height = 20;
        }

        public void updateTitle() {
            title = playerLM.getProfile().getName();
            hover = null;

            if (gui.getFontRenderer().getStringWidth(title) > width - 24) {
                hover = title + "";
                title = gui.getFontRenderer().trimStringToWidth(title, width - 22) + "...";
            }
        }

        public void onButtonPressed(int b) {
            ClientAction.REQUEST_PLAYER_INFO.send(playerLM.getPlayerID());
            super.onButtonPressed(b);
        }

        public void renderWidget() {
            int ay = getAY();
            if (ay < -height || ay > guiGuide.mainPanel.height) return;
            int ax = getAX();

            double z = gui.getZLevel();

            if (mouseOver()) {
                GlStateManager.color(1F, 1F, 1F, 0.2F);
                GuiLM.drawBlankRect(ax, ay, z, width, height);
            }

            boolean raw1 = playerLM.isFriendRaw(LMWorldClient.inst.clientPlayer);
            boolean raw2 = LMWorldClient.inst.clientPlayer.isFriendRaw(playerLM);

            GlStateManager.color(0F, 0F, 0F, 1F);
            if (raw1 && raw2) GlStateManager.color(0.18F, 0.74F, 0.18F, 1F);
            // else if(raw1 || raw2) GlStateManager.color(raw1 ? 0xFFE0BE00 : 0xFF00B6ED);
            else if (raw1) GlStateManager.color(0.87F, 0.74F, 0F, 1F);
            else if (raw2) GlStateManager.color(0F, 0.71F, 0.92F, 1F);

            GuiLM.drawBlankRect(ax + 1, ay + 1, z, 18, 18);

            GlStateManager.color(1F, 1F, 1F, 1F);
            GuiLM.drawPlayerHead(playerLM.getProfile().getName(), ax + 2, ay + 2, 16, 16, z);

            gui.getFontRenderer().drawString(title, ax + 22, ay + 6, playerLM.isOnline() ? 0xFF11FF11 : 0xFFFFFFFF);
        }
    }
}
