package serverutils.serverlib.client;


import org.apache.logging.log4j.Level;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiMessageDialog;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.Event;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.client.ClientUtils;
import serverutils.serverlib.lib.gui.Button;
import serverutils.serverlib.lib.gui.GuiHelper;
import serverutils.serverlib.lib.gui.GuiIcons;
import serverutils.serverlib.lib.gui.Panel;
import serverutils.serverlib.lib.gui.SimpleTextButton;
import serverutils.serverlib.lib.gui.WidgetType;
import serverutils.serverlib.lib.gui.misc.GuiButtonListBase;
import serverutils.serverlib.lib.gui.misc.GuiLoading;
import serverutils.serverlib.lib.icon.Icon;
import serverutils.serverlib.lib.util.SidedUtils;
import serverutils.serverlib.lib.util.misc.MouseButton;

import java.util.ArrayList;
import java.util.List;

public class GuiClientConfig extends GuiButtonListBase {
	private class GuiCustomConfig extends GuiConfig {
		public GuiCustomConfig(String modid, String title) {
			super(Minecraft.getMinecraft().currentScreen, null, modid, false, false, title);
		}

		@Override
		protected void actionPerformed(GuiButton button) {
			if (button.id == 2000) {
				boolean flag = true;
				try {
					if ((configID != null || !(parentScreen instanceof GuiConfig)) && entryList.hasChangedEntry(true)) {
						boolean requiresMcRestart = entryList.saveConfigElements();

						ConfigChangedEvent event = new ConfigChangedEvent.OnConfigChangedEvent(modID, configID, isWorldRunning, requiresMcRestart);
						MinecraftForge.EVENT_BUS.post(event);
						if (!event.getResult().equals(Event.Result.DENY)) {
							MinecraftForge.EVENT_BUS.post(new ConfigChangedEvent.PostConfigChangedEvent(modID, configID, isWorldRunning, requiresMcRestart));
						}

						if (requiresMcRestart) {
							flag = false;
							mc.displayGuiScreen(new GuiMessageDialog(parentScreen, "fml.configgui.gameRestartTitle", new ChatComponentText(I18n.format("fml.configgui.gameRestartRequired")), "fml.configgui.confirmRestartMessage"));
						}

						if (parentScreen instanceof GuiConfig) {
							((GuiConfig) parentScreen).needsRefresh = true;
						}
					}
				}
				catch (Throwable e) {
					FMLLog.log(Level.ERROR, "Error performing GuiConfig action:", e);
				}

				if (flag) {
					mc.displayGuiScreen(parentScreen);
				}
			}
			else {
				super.actionPerformed(button);
			}
		}
	}

	private class ButtonClientConfig extends SimpleTextButton {
		private final String modId;

		public ButtonClientConfig(Panel panel, ClientConfig config) {
			super(panel, config.name.getFormattedText(), config.icon);
			modId = config.id;
		}

		@Override
		public void onClicked(MouseButton button) {
			GuiHelper.playClickSound();
			Minecraft.getMinecraft().displayGuiScreen(new GuiCustomConfig(modId, getTitle()));
		}
	}

	public GuiClientConfig()
	{
		setTitle(I18n.format("sidebar_button.ftblib.settings"));
	}

	@Override
	public void addButtons(Panel panel) {
		panel.add(new SimpleTextButton(panel, I18n.format("player_config"), GuiIcons.SETTINGS_RED) {
			@Override
			public void onClicked(MouseButton button) {
				GuiHelper.playClickSound();
				new GuiLoading().openGui();
				ClientUtils.execClientCommand("/my_settings");
			}

			@Override
			public WidgetType getWidgetType() {
				return SidedUtils.isModLoadedOnServer(ServerLib.MOD_ID) ? super.getWidgetType() : WidgetType.DISABLED;
			}
		});

		panel.add(new SimpleTextButton(panel, I18n.format("sidebar_button"), Icon.getIcon("ftblib:textures/gui/teams.png")) {
			@Override
			public void onClicked(MouseButton button) {
				GuiHelper.playClickSound();
				new GuiSidebarButtonConfig().openGui();
			}
		});

		List<Button> buttons = new ArrayList<>();

		for (ClientConfig config : ServerLibClient.CLIENT_CONFIG_MAP.values()) {
			buttons.add(new ButtonClientConfig(panel, config));
		}

		buttons.sort((o1, o2) -> o1.getTitle().compareToIgnoreCase(o2.getTitle()));
		panel.addAll(buttons);
	}

	@Override
	public void onClosed() {
		super.onClosed();
		SidebarButtonManager.INSTANCE.saveConfig();
	}
}