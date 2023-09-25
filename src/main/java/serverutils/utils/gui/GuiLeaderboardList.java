package serverutils.utils.gui;

import java.util.Map;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import com.feed_the_beast.ftblib.lib.gui.GuiHelper;
import com.feed_the_beast.ftblib.lib.gui.Panel;
import com.feed_the_beast.ftblib.lib.gui.SimpleTextButton;
import com.feed_the_beast.ftblib.lib.gui.misc.GuiButtonListBase;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftblib.lib.util.misc.MouseButton;
import serverutils.utils.net.MessageLeaderboard;

/**
 * @author LatvianModder
 */
public class GuiLeaderboardList extends GuiButtonListBase {

    private final Map<ResourceLocation, IChatComponent> leaderboards;

    public GuiLeaderboardList(Map<ResourceLocation, IChatComponent> l) {
        leaderboards = l;
        setTitle(I18n.format("sidebar_button.ftbutilities.leaderboards"));
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
