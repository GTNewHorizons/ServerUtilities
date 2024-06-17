package serverutils.client.gui.ranks;

import java.util.List;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import serverutils.lib.client.ClientUtils;
import serverutils.lib.config.ConfigValueInstance;
import serverutils.lib.gui.CheckBoxList;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.util.StringUtils;
import serverutils.ranks.Rank;

public class GuiSelectRank extends GuiButtonListBase {

    private final RankInst playerRank;
    private final String username;

    public GuiSelectRank(String username, RankInst p) {
        playerRank = p;
        this.username = username;
        setTitle(StatCollector.translateToLocal("serverutilities.admin_panel.ranks.select_rank"));
        setHasSearchBox(true);
    }

    @Override
    public void onPostInit() {
        panelButtons.refreshWidgets();
        super.onPostInit();
    }

    @Override
    public void addButtons(Panel panel) {
        panel.add(new RankList(panel));
    }

    public class RankList extends CheckBoxList {

        public RankList(Panel parent) {
            super(parent, false);
            for (RankInst rank : GuiRanks.ranks.values()) {
                int defaultRank = 0;
                for (ConfigValueInstance inst : rank.group.getValues()) {
                    if (inst.getId().equals(Rank.NODE_DEFAULT_PLAYER)) {
                        defaultRank += 1;
                    }
                    if (inst.getId().equals(Rank.NODE_DEFAULT_OP)) {
                        defaultRank += 2;
                    }
                }

                String rankId = rank.getId();
                CheckBoxList.CheckBoxEntry entry = new RankEntry(rankId, defaultRank)
                        .setDisplayName(StringUtils.firstUppercase(rankId));
                if (playerRank.parents.contains(rankId)) {
                    entry.value = 1;
                }

                ConfigValueInstance val = playerRank.group.getValueInstance("is_op");
                boolean isPlayerOp = val != null && val.getValue().getBoolean();
                if (isPlayerOp && defaultRank >= 2) {
                    entry.setLocked(true);
                }

                addBox(entry);
            }
        }
    }

    public class RankEntry extends CheckBoxList.CheckBoxEntry {

        public RankEntry(String n, int defaultRank) {
            super(n);
            if (defaultRank == 1) {
                setLocked(true);
            }
        }

        @Override
        public void addMouseOverText(List<String> list) {
            if (locked) {
                list.add(StatCollector.translateToLocal("serverutilities.admin_panel.ranks.cant_remove"));
            }
            super.addMouseOverText(list);
        }

        @Override
        public String getDisplayName() {
            if (locked) {
                return EnumChatFormatting.GOLD + displayName;
            }
            return super.getDisplayName();
        }

        @Override
        public void onValueChanged() {
            if (value > 0) {
                playerRank.parents.add(name);
                ClientUtils.execClientCommand("/ranks add " + username + " " + name);
            } else {
                playerRank.parents.remove(name);
                ClientUtils.execClientCommand("/ranks remove " + username + " " + name);
            }
        }
    }
}
