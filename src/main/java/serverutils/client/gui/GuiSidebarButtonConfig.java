package serverutils.client.gui;

import java.util.List;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.registry.LanguageRegistry;
import serverutils.client.EnumSidebarLocation;
import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.Theme;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.icon.Color4I;
import serverutils.lib.util.misc.MouseButton;

public class GuiSidebarButtonConfig extends GuiButtonListBase {

    private static final Color4I COLOR_ENABLED = Color4I.rgba(0x5547BF41);
    private static final Color4I COLOR_UNAVAILABLE = Color4I.rgba(0x550094FF);
    private static final Color4I COLOR_DISABLED = Color4I.rgba(0x55BC4242);

    private static class ButtonConfigSidebarButton extends SimpleTextButton {

        private final SidebarButton sidebarButton;
        private String tooltip = "";

        public ButtonConfigSidebarButton(Panel panel, SidebarButton s) {
            super(panel, I18n.format(s.getLangKey()), s.getIcon());
            sidebarButton = s;
            if (!LanguageRegistry.instance().getStringLocalization(s.getTooltipLangKey()).isEmpty()) {
                tooltip = I18n.format(s.getTooltipLangKey());
            }
        }

        @Override
        public void addMouseOverText(List<String> list) {
            list.add(
                    sidebarButton.getConfig() ? I18n.format("addServer.resourcePack.enabled")
                            : I18n.format("addServer.resourcePack.disabled"));

            if (!tooltip.isEmpty()) {
                list.add(tooltip);
            }
        }

        @Override
        public void drawBackground(Theme theme, int x, int y, int w, int h) {
            super.drawBackground(theme, x, y, w, h);
            (sidebarButton.getConfig() ? (sidebarButton.isVisible() ? COLOR_ENABLED : COLOR_UNAVAILABLE)
                    : COLOR_DISABLED).draw(x, y, w, h);
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();

            if (isCtrlKeyDown()) {
                sidebarButton.onClicked(isShiftKeyDown());
            } else {
                sidebarButton.setConfig(!sidebarButton.getConfig());
            }
        }
    }

    private static class ButtonResetSidebar extends SimpleTextButton {

        public ButtonResetSidebar(Panel panel) {
            super(panel, StatCollector.translateToLocal("serverutilities.sidebar_button.reset"), GuiIcons.REFRESH);
        }

        @Override
        public void addMouseOverText(List<String> list) {
            list.add(StatCollector.translateToLocal("serverutilities.sidebar_button.reset.tooltip"));
        }

        @Override
        public void drawBackground(Theme theme, int x, int y, int w, int h) {
            super.drawBackground(theme, x, y, w, h);
            COLOR_ENABLED.draw(x, y, w, h);
        }

        @Override
        public void onClicked(MouseButton button) {
            GuiHelper.playClickSound();
            GuiSidebar.dragOffsetX = 0;
            GuiSidebar.dragOffsetY = 0;
        }
    }

    public GuiSidebarButtonConfig() {
        setTitle(I18n.format("sidebar_button"));
    }

    @Override
    public void addButtons(Panel panel) {
        if (ServerUtilitiesClientConfig.sidebar_buttons == EnumSidebarLocation.UNLOCKED) {
            panel.add(new ButtonResetSidebar(panel));
        }
        for (SidebarButtonGroup group : SidebarButtonManager.INSTANCE.groups) {
            for (SidebarButton button : group.getButtons()) {
                if (!button.isVisible()) continue;
                panel.add(new ButtonConfigSidebarButton(panel, button));
            }
        }

    }

    @Override
    public void onClosed() {
        super.onClosed();
        SidebarButtonManager.INSTANCE.saveConfig();
    }
}
