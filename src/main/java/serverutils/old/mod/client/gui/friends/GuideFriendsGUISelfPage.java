package serverutils.old.mod.client.gui.friends;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import com.google.gson.JsonPrimitive;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.PrivacyLevel;
import serverutils.lib.api.PlayerAction;
import serverutils.lib.api.gui.PlayerActionRegistry;
import serverutils.lib.api.notification.ClickAction;
import serverutils.lib.api.notification.ClickActionType;
import serverutils.old.api.guide.GuidePage;
import serverutils.old.api.guide.lines.GuideExtendedTextLine;
import serverutils.old.mod.ServerUtilities;
import serverutils.old.world.LMWorldClient;
import serverutils.old.world.PersonalSettings;

/**
 * Created by LatvianModder on 24.03.2016.
 */
@SideOnly(Side.CLIENT)
public class GuideFriendsGUISelfPage extends GuideFriendsGUIPage {

    public GuideFriendsGUISelfPage() {
        super(LMWorldClient.inst.clientPlayer);
        onClientDataChanged();
    }

    public void onClientDataChanged() {
        clear();

        for (PlayerAction a : PlayerActionRegistry.getPlayerActions(
                PlayerAction.Type.SELF,
                LMWorldClient.inst.clientPlayer,
                LMWorldClient.inst.clientPlayer,
                true,
                true)) {
            text.add(new GuidePlayerActionLine(this, playerLM, a));
        }

        GuidePage page = getSub("info").setTitle(new ChatComponentTranslation("serverlib.button.info"));

        page.text.add(new GuidePlayerViewLine(this, playerLM));

        if (!playerLM.clientInfo.isEmpty()) {
            for (String s : playerLM.clientInfo) page.printlnText(s);

            page.text.add(null);
        }

        page = new GuidePage("settings") {

            public void onClientDataChanged() {
                clear();

                PersonalSettings ps = LMWorldClient.inst.clientPlayer.getSettings();

                booleanCommand("chat_links", ps.get(PersonalSettings.CHAT_LINKS));
                booleanCommand("render_badge", LMWorldClient.inst.clientPlayer.renderBadge);
                booleanCommand("explosions", ps.get(PersonalSettings.EXPLOSIONS));
                booleanCommand("fake_players", ps.get(PersonalSettings.FAKE_PLAYERS));

                IChatComponent text1 = ps.blocks.lang.chatComponent();
                text1.getChatStyle().setColor(
                        ps.blocks == PrivacyLevel.FRIENDS ? EnumChatFormatting.BLUE
                                : (ps.blocks == PrivacyLevel.PUBLIC ? EnumChatFormatting.GREEN
                                        : EnumChatFormatting.RED));
                GuideExtendedTextLine line = new GuideExtendedTextLine(
                        this,
                        ServerUtilities.mod.chatComponent("player_setting.security_level").appendText(": ")
                                .appendSibling(text1));
                line.setClickAction(
                        new ClickAction(
                                ClickActionType.CMD,
                                new JsonPrimitive("lmplayer_settings block_security toggle")));
                text.add(line);
            }

            private void booleanCommand(String s, boolean current) {
                ChatComponentText text1 = new ChatComponentText(Boolean.toString(current));
                text1.getChatStyle().setColor(current ? EnumChatFormatting.GREEN : EnumChatFormatting.RED);
                GuideExtendedTextLine line = new GuideExtendedTextLine(
                        this,
                        ServerUtilities.mod.chatComponent("player_setting." + s).appendText(": ").appendSibling(text1));
                line.setClickAction(
                        new ClickAction(ClickActionType.CMD, new JsonPrimitive("lmplayer_settings " + s + " toggle")));
                text.add(line);
            }
        };

        page.setTitle(new ChatComponentTranslation("serverlib.button.settings"));
        page.onClientDataChanged();
        addSub(page);
    }
}
