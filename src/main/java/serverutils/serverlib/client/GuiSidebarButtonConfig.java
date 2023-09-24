package serverutils.serverlib.client;

import serverutils.serverlib.lib.gui.GuiHelper;
import serverutils.serverlib.lib.gui.Panel;
import serverutils.serverlib.lib.gui.SimpleTextButton;
import serverutils.serverlib.lib.gui.Theme;
import serverutils.serverlib.lib.gui.misc.GuiButtonListBase;
import serverutils.serverlib.lib.icon.Color4I;
import serverutils.serverlib.lib.util.misc.MouseButton;
import net.minecraft.client.resources.I18n;

import java.util.List;

public class GuiSidebarButtonConfig extends GuiButtonListBase {
	private static final Color4I COLOR_ENABLED = Color4I.rgba(0x5547BF41);
	private static final Color4I COLOR_UNAVAILABLE = Color4I.rgba(0x550094FF);
	private static final Color4I COLOR_DISABLED = Color4I.rgba(0x55BC4242);

	private class ButtonConfigSidebarButton extends SimpleTextButton {
		private final SidebarButton sidebarButton;
		private String tooltip = "";

		public ButtonConfigSidebarButton(Panel panel, SidebarButton s) {
			super(panel, I18n.format(s.getLangKey()), s.getIcon());
			sidebarButton = s;

			//if (I18n.hasKey(s.getTooltipLangKey())) {
				tooltip = I18n.format(s.getTooltipLangKey());
			//}
		}

		@Override
		public void addMouseOverText(List<String> list) {
			list.add(sidebarButton.getConfig() ? I18n.format("addServer.resourcePack.enabled") : I18n.format("addServer.resourcePack.disabled"));

			if (!tooltip.isEmpty()) {
				list.add(tooltip);
			}
		}

		@Override
		public void drawBackground(Theme theme, int x, int y, int w, int h) {
			super.drawBackground(theme, x, y, w, h);
			(sidebarButton.getConfig() ? (sidebarButton.isVisible() ? COLOR_ENABLED : COLOR_UNAVAILABLE) : COLOR_DISABLED).draw(x, y, w, h);
		}

		@Override
		public void onClicked(MouseButton button) {
			GuiHelper.playClickSound();

			if (isCtrlKeyDown()) {
				sidebarButton.onClicked(isShiftKeyDown());
			}
			else {
				sidebarButton.setConfig(!sidebarButton.getConfig());
			}
		}
	}

	public GuiSidebarButtonConfig()
	{
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