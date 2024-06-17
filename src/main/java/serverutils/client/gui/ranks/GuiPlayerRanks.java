package serverutils.client.gui.ranks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.util.StatCollector;

import serverutils.lib.gui.Button;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.Theme;
import serverutils.lib.gui.Widget;
import serverutils.lib.gui.WidgetType;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.misc.MouseButton;

public class GuiPlayerRanks extends GuiButtonListBase {

    private int usernameSize, valueSize;

    private class PlayerEntry extends Button implements Comparable<PlayerEntry> {

        private final String username;
        private final String ranks;
        private final RankInst playerRank;

        public PlayerEntry(Panel panel, String u, RankInst r) {
            super(panel);
            username = u;
            playerRank = r;
            List<String> sortedParents = r.parents.stream().sorted(String::compareToIgnoreCase)
                    .collect(Collectors.toList());
            ranks = getRanksAsString(sortedParents);

            Theme theme = getTheme();
            usernameSize = Math.max(usernameSize, theme.getStringWidth(username) + 8);
            valueSize = Math.max(valueSize, theme.getStringWidth(ranks) + 8);

            setSize(usernameSize + valueSize, 14);
        }

        private String getRanksAsString(List<String> parents) {
            StringBuilder builder = new StringBuilder();
            for (String ranks : parents) {
                builder.append(StringUtils.firstUppercase(ranks)).append(", ");
            }

            int index = builder.lastIndexOf(", ");
            if (index >= 0) {
                builder.delete(index, index + 2);
            }

            return builder.toString();
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            new GuiSelectRank(username, playerRank).openGui();
        }

        @Override
        public void addMouseOverText(List<String> list) {}

        @Override
        public void draw(Theme theme, int x, int y, int w, int h) {
            WidgetType type = WidgetType.mouseOver(isMouseOver());
            int textY = y + (h - theme.getFontHeight() + 1) / 2;

            theme.drawButton(x, y, usernameSize, h, type);
            theme.drawString(username, x + 4, textY, Theme.SHADOW);

            theme.drawButton(x + usernameSize, y, valueSize, h, type);
            theme.drawString(ranks, x + usernameSize + 4, textY, Theme.SHADOW);
        }

        @Override
        public int compareTo(PlayerEntry o) {
            return username.compareToIgnoreCase(o.username);
        }
    }

    public GuiPlayerRanks() {
        setTitle(StatCollector.translateToLocal("serverutilities.admin_panel.ranks.player_ranks"));
        setHasSearchBox(true);
    }

    @Override
    public void addButtons(Panel panel) {
        usernameSize = 0;
        valueSize = 0;

        for (Map.Entry<String, RankInst> entry : GuiRanks.playerRanks.entrySet()) {
            panel.add(new PlayerEntry(panel, entry.getKey(), entry.getValue()));
        }

        panel.widgets.sort(null);
    }

    @Override
    public String getFilterText(Widget widget) {
        return ((PlayerEntry) widget).username.toLowerCase();
    }
}
