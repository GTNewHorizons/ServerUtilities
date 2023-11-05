package serverutils.client.gui;

import java.util.Map;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageLeaderboard;

public class GuiLeaderboardList extends GuiButtonListBase {

    private final Map<ResourceLocation, IChatComponent> leaderboards;

    public GuiLeaderboardList(Map<ResourceLocation, IChatComponent> l) {
        leaderboards = l;
        setTitle(I18n.format("sidebar_button.serverutilities.leaderboards"));
    }

    @Override
    public void addButtons(Panel panel) {
        for (Map.Entry<ResourceLocation, IChatComponent> entry : leaderboards.entrySet()) {
            panel.add(new SimpleTextButton(panel, entry.getValue().getFormattedText(), Icon.EMPTY) {

                @Override
                public void onClicked(MouseButton button) {
                    GuiHelper.playClickSound();
                    new MessageLeaderboard(entry.getKey()).sendToServer();
                }
            });
        }
    }
}
