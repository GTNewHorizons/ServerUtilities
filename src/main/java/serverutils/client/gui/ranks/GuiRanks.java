package serverutils.client.gui.ranks;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.StatCollector;

import serverutils.lib.client.ClientUtils;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.ConfigValueInstance;
import serverutils.lib.gui.ContextMenuItem;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.icon.PlayerHeadIcon;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.misc.MouseButton;

public class GuiRanks extends GuiButtonListBase {

    public static Map<String, RankInst> ranks;
    public static Map<String, RankInst> playerRanks;
    public static ConfigGroup allPerms;
    public static ConfigGroup commandPerms;

    public GuiRanks(Collection<RankInst> r, Map<String, RankInst> p, ConfigGroup allPerm, ConfigGroup commands) {
        ranks = new LinkedHashMap<>();
        playerRanks = p;

        for (RankInst inst : r) {
            ranks.put(inst.getId(), inst);
        }

        if (allPerms == null) {
            allPerms = allPerm;
        }
        if (commandPerms == null) {
            commandPerms = commands;
        }

    }

    @Override
    public void addButtons(Panel panel) {
        panel.add(
                new SimpleTextButton(
                        panel,
                        StatCollector.translateToLocal("serverutilities.admin_panel.ranks.player_ranks"),
                        new PlayerHeadIcon(null).withPadding(4)) {

                    @Override
                    public void onClicked(MouseButton button) {
                        GuiHelper.playClickSound();
                        new GuiPlayerRanks().openGui();
                    }
                });

        panel.add(new SimpleTextButton(panel, I18n.format("gui.add"), GuiIcons.ADD) {

            @Override
            public void onClicked(MouseButton button) {
                GuiHelper.playClickSound();
                new GuiAddRank(GuiRanks.this).openGui();
            }
        });
        for (RankInst inst : ranks.values()) {
            panel.add(addRankButton(panel, inst));
        }
    }

    public void addRank(RankInst inst) {
        ranks.computeIfAbsent(inst.getId(), k -> {
            addRankButton(panelButtons, inst);
            refreshWidgets();
            return inst;
        });
    }

    public SimpleTextButton addRankButton(Panel panel, RankInst inst) {
        return new SimpleTextButton(panel, StringUtils.firstUppercase(inst.getId()), GuiIcons.SETTINGS) {

            @Override
            public void onClicked(MouseButton button) {
                ContextMenuItem item = new ContextMenuItem(
                        StatCollector.translateToLocal("selectServer.delete"),
                        GuiIcons.REMOVE,
                        () -> openYesNo(
                                StatCollector.translateToLocalFormatted(
                                        "serverutilities.admin_panel.ranks.delete_confirm",
                                        inst.getId()),
                                "",
                                () -> removeRank(this)));
                GuiHelper.playClickSound();
                if (button == MouseButton.RIGHT) {
                    GuiRanks.this.openContextMenu(Collections.singletonList(item));
                } else {
                    new GuiEditRank(inst).openGui();
                }
            }
        };
    }

    public void removeRank(SimpleTextButton btn) {
        ClientUtils.execClientCommand("/ranks delete " + btn.getTitle().toLowerCase());
        ranks.remove(btn.getTitle().toLowerCase());
        panelButtons.widgets.remove(btn);
        refreshWidgets();
    }

    @Nullable
    public static ConfigValueInstance getValue(String id) {
        ConfigValueInstance inst = allPerms.getValueInstance(id);
        if (inst == null) {
            inst = commandPerms.getValueInstance(id);
        }
        return inst;
    }
}
