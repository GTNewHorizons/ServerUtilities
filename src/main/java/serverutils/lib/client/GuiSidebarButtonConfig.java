package serverutils.lib.client;

import java.util.List;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.common.registry.LanguageRegistry;
import serverutils.lib.lib.gui.GuiHelper;
import serverutils.lib.lib.gui.Panel;
import serverutils.lib.lib.gui.SimpleTextButton;
import serverutils.lib.lib.gui.Theme;
import serverutils.lib.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.lib.icon.Color4I;
import serverutils.lib.lib.util.misc.MouseButton;

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

    public GuiSidebarButtonConfig() {
        setTitle(I18n.format("sidebar_button"));
    }

    @Override
    public void addButtons(Panel panel) {
        for (SidebarButtonGroup group : SidebarButtonManager.INSTANCE.groups) {
            for (SidebarButton button : group.getButtons()) {
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
