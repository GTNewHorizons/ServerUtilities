package serverutils.serverlib.lib.gui.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import serverutils.serverlib.client.GlStateManager;
import serverutils.serverlib.lib.config.ConfigGroup;
import serverutils.serverlib.lib.config.ConfigValueInstance;
import serverutils.serverlib.lib.config.IConfigCallback;
import serverutils.serverlib.lib.gui.Button;
import serverutils.serverlib.lib.gui.GuiBase;
import serverutils.serverlib.lib.gui.GuiHelper;
import serverutils.serverlib.lib.gui.GuiIcons;
import serverutils.serverlib.lib.gui.Panel;
import serverutils.serverlib.lib.gui.PanelScrollBar;
import serverutils.serverlib.lib.gui.SimpleButton;
import serverutils.serverlib.lib.gui.Theme;
import serverutils.serverlib.lib.gui.Widget;
import serverutils.serverlib.lib.gui.WidgetLayout;
import serverutils.serverlib.lib.gui.WidgetType;
import serverutils.serverlib.lib.icon.Color4I;
import serverutils.serverlib.lib.icon.MutableColor4I;
import serverutils.serverlib.lib.io.Bits;
import serverutils.serverlib.lib.util.misc.MouseButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GuiEditConfig extends GuiBase
{
	public static final Color4I COLOR_BACKGROUND = Color4I.rgba(0x99333333);
	public static final Comparator<ConfigValueInstance> COMPARATOR = (o1, o2) -> {
		int i = o1.getGroup().getPath().compareToIgnoreCase(o2.getGroup().getPath());

		if (i == 0)
		{
			i = Integer.compare(o1.getOrder(), o2.getOrder());
		}

		if (i == 0)
		{
			i = o1.getDisplayName().getUnformattedText().compareToIgnoreCase(o2.getDisplayName().getUnformattedText());
		}

		return i;
	};

	public static Theme THEME = new Theme()
	{
		@Override
		public void drawScrollBarBackground(int x, int y, int w, int h, WidgetType type)
		{
			Color4I.BLACK.withAlpha(70).draw(x, y, w, h);
		}

		@Override
		public void drawScrollBar(int x, int y, int w, int h, WidgetType type, boolean vertical)
		{
			getContentColor(WidgetType.NORMAL).withAlpha(100).withBorder(Color4I.GRAY.withAlpha(100), false).draw(x, y, w, h);
		}
	};

	public class ButtonConfigGroup extends Button
	{
		public final ConfigGroup group;
		public String title, info;
		public boolean collapsed = false;

		public ButtonConfigGroup(Panel panel, ConfigGroup g)
		{
			super(panel);
			setHeight(12);
			group = g;

			if (group.parent != null)
			{
				List<ConfigGroup> groups = new ArrayList<>();

				g = group;

				do
				{
					groups.add(g);
					g = g.parent;
				}
				while (g != null);

				groups.remove(groups.size() - 1);

				StringBuilder builder = new StringBuilder();

				for (int i = groups.size() - 1; i >= 0; i--)
				{
					builder.append(groups.get(i).getDisplayName().getFormattedText());

					if (i != 0)
					{
						builder.append(" > ");
					}
				}

				title = builder.toString();
			}
			else
			{
				title = I18n.format("stat.generalButton");
			}

			String infoKey = group.getPath() + ".info";
			info = I18n.hasKey(infoKey) ? I18n.format(infoKey) : "";
			setCollapsed(collapsed);
		}

		public void setCollapsed(boolean v)
		{
			collapsed = v;
			setTitle((collapsed ? (EnumChatFormatting.RED + "[-] ") : (EnumChatFormatting.GREEN + "[v] ")) + EnumChatFormatting.RESET + title);
		}

		@Override
		public void draw(Theme theme, int x, int y, int w, int h)
		{
			COLOR_BACKGROUND.draw(x, y, w, h);
			theme.drawString(getTitle(), x + 2, y + 2);
			GlStateManager.color(1F, 1F, 1F, 1F);

			if (isMouseOver())
			{
				Color4I.WHITE.withAlpha(33).draw(x, y, w, h);
			}
		}

		@Override
		public void addMouseOverText(List<String> list)
		{
			if (!info.isEmpty())
			{
				list.add(info);
			}
		}

		@Override
		public void onClicked(MouseButton button)
		{
			setCollapsed(!collapsed);
			getGui().refreshWidgets();
		}
	}

	private class ButtonConfigEntry extends Button
	{
		public final ButtonConfigGroup group;
		public final ConfigValueInstance inst;
		public String keyText;
		private String valueString = null;

		public ButtonConfigEntry(Panel panel, ButtonConfigGroup g, ConfigValueInstance i)
		{
			super(panel);
			setHeight(12);
			group = g;
			inst = i;

			if (!inst.getCanEdit())
			{
				IChatComponent c = inst.getDisplayName().createCopy();
				c.getChatStyle().setColor(EnumChatFormatting.GRAY);
				keyText = c.getFormattedText();
			}
			else
			{
				keyText = inst.getDisplayName().getFormattedText();
			}
		}

		public String getValueString()
		{
			if (valueString == null)
			{
				valueString = inst.getValue().getStringForGUI().getFormattedText();
			}

			return valueString;
		}

		@Override
		public void draw(Theme theme, int x, int y, int w, int h)
		{
			boolean mouseOver = getMouseY() >= 20 && isMouseOver();

			if (mouseOver)
			{
				Color4I.WHITE.withAlpha(33).draw(x, y, w, h);
			}

			theme.drawString(keyText, x + 4, y + 2, Bits.setFlag(0, Theme.SHADOW, mouseOver));
			GlStateManager.color(1F, 1F, 1F, 1F);

			String s = getValueString();
			int slen = theme.getStringWidth(s);

			if (slen > 150)
			{
				s = theme.trimStringToWidth(s, 150) + "...";
				slen = 152;
			}

			MutableColor4I textCol = inst.getValue().getColor().mutable();
			textCol.setAlpha(255);

			if (mouseOver)
			{
				textCol.addBrightness(60);

				if (getMouseX() > x + w - slen - 9)
				{
					Color4I.WHITE.withAlpha(33).draw(x + w - slen - 8, y, slen + 8, h);
				}
			}

			theme.drawString(s, getGui().width - (slen + 20), y + 2, textCol, 0);
			GlStateManager.color(1F, 1F, 1F, 1F);
		}

		@Override
		public void onClicked(MouseButton button)
		{
			if (getMouseY() >= 20)
			{
				GuiHelper.playClickSound();
				inst.getValue().onClicked(GuiEditConfig.this, inst, button, () -> valueString = null);
			}
		}

		@Override
		public void addMouseOverText(List<String> list)
		{
			if (getMouseY() > 18)
			{
				list.add(EnumChatFormatting.UNDERLINE + keyText);
				IChatComponent infoText = inst.getInfo();

				if (!(infoText instanceof ChatComponentTranslation) || I18n.hasKey(((ChatComponentTranslation) infoText).getKey()))
				{
					for (String s : infoText.getFormattedText().split("\\\n"))
					{
						list.add(EnumChatFormatting.GRAY.toString() + EnumChatFormatting.ITALIC + s);
					}
				}

				list.add("");
				inst.getValue().addInfo(inst, list);
			}
		}
	}

	private final ConfigGroup group;
	private final ConfigGroup originalGroup;
	private final IConfigCallback callback;

	private final String title;
	private final List<Widget> configEntryButtons;
	private final Panel configPanel;
	private final Button buttonAccept, buttonCancel, buttonCollapseAll, buttonExpandAll;
	private final PanelScrollBar scroll;
	private int shouldClose = 0;
	private int groupSize = 0;

	public GuiEditConfig(ConfigGroup g, IConfigCallback c)
	{
		group = g;
		originalGroup = group.copy();
		callback = c;

		IChatComponent title0 = g.getDisplayName().createCopy();
		title0.getChatStyle().setBold(true);
		title = title0.getFormattedText();

		configEntryButtons = new ArrayList<>();

		configPanel = new Panel(this)
		{
			@Override
			public void addWidgets()
			{
				for (Widget w : configEntryButtons)
				{
					if (!(w instanceof ButtonConfigEntry) || !((ButtonConfigEntry) w).group.collapsed)
					{
						add(w);
					}
				}
			}

			@Override
			public void alignWidgets()
			{
				for (Widget w : widgets)
				{
					w.setWidth(width - 16);
				}

				scroll.setMaxValue(align(WidgetLayout.VERTICAL));
			}
		};

		List<ConfigValueInstance> list = new ArrayList<>();
		collectAllConfigValues(group, list);

		if (!list.isEmpty())
		{
			list.sort(COMPARATOR);

			ButtonConfigGroup group = null;

			for (ConfigValueInstance instance : list)
			{
				if (group == null || !group.group.equals(instance.getGroup()))
				{
					group = new ButtonConfigGroup(configPanel, instance.getGroup());
					configEntryButtons.add(group);
					groupSize++;
				}

				configEntryButtons.add(new ButtonConfigEntry(configPanel, group, instance));
			}

			if (groupSize == 1)
			{
				configEntryButtons.remove(group);
			}
		}

		scroll = new PanelScrollBar(this, configPanel);

		buttonAccept = new SimpleButton(this, I18n.format("gui.close"), GuiIcons.ACCEPT, (widget, button) ->
		{
			shouldClose = 1;
			widget.getGui().closeGui();
		});

		buttonCancel = new SimpleButton(this, I18n.format("gui.cancel"), GuiIcons.CANCEL, (widget, button) ->
		{
			shouldClose = 2;
			widget.getGui().closeGui();
		});

		buttonExpandAll = new SimpleButton(this, I18n.format("gui.expand_all"), GuiIcons.ADD, (widget, button) ->
		{
			for (Widget w : configEntryButtons)
			{
				if (w instanceof ButtonConfigGroup)
				{
					((ButtonConfigGroup) w).setCollapsed(false);
				}
			}

			scroll.setValue(0);
			widget.getGui().refreshWidgets();
		});

		buttonCollapseAll = new SimpleButton(this, I18n.format("gui.collapse_all"), GuiIcons.REMOVE, (widget, button) ->
		{
			for (Widget w : configEntryButtons)
			{
				if (w instanceof ButtonConfigGroup)
				{
					((ButtonConfigGroup) w).setCollapsed(true);
				}
			}

			scroll.setValue(0);
			widget.getGui().refreshWidgets();
		});
	}

	private void collectAllConfigValues(ConfigGroup group, List<ConfigValueInstance> list)
	{
		for (ConfigValueInstance instance : group.getValues())
		{
			if (!instance.getHidden())
			{
				list.add(instance);
			}
		}

		for (ConfigGroup group1 : group.getGroups())
		{
			collectAllConfigValues(group1, list);
		}
	}

	@Override
	public boolean onInit()
	{
		for (Widget widget : configEntryButtons)
		{
			if (widget instanceof ButtonConfigEntry)
			{
				((ButtonConfigEntry) widget).valueString = null;
			}
		}

		return setFullscreen();
	}

	@Override
	public void addWidgets()
	{
		add(buttonAccept);
		add(buttonCancel);

		if (groupSize > 1)
		{
			add(buttonExpandAll);
			add(buttonCollapseAll);
		}

		add(configPanel);
		add(scroll);
	}

	@Override
	public void alignWidgets()
	{
		configPanel.setPosAndSize(0, 20, width, height - 20);
		configPanel.alignWidgets();
		scroll.setPosAndSize(width - 16, 20, 16, height - 20);

		buttonAccept.setPos(width - 18, 2);
		buttonCancel.setPos(width - 38, 2);

		if (groupSize > 1)
		{
			buttonExpandAll.setPos(width - 58, 2);
			buttonCollapseAll.setPos(width - 78, 2);
		}
	}

	@Override
	public void onClosed()
	{
		super.onClosed();

		if (shouldClose != 1)
		{
			group.deserializeNBT(originalGroup.serializeNBT());
		}

		callback.onConfigSaved(group, Minecraft.getMinecraft().thePlayer);
	}

	@Override
	public boolean onClosedByKey(int key)
	{
		if (super.onClosedByKey(key))
		{
			shouldClose = 1;
			closeGui();
		}

		return false;
	}

	@Override
	public void drawBackground(Theme theme, int x, int y, int w, int h)
	{
		COLOR_BACKGROUND.draw(0, 0, w, 20);
		theme.drawString(getTitle(), 6, 6, Theme.SHADOW);
	}

	@Override
	public String getTitle()
	{
		return title;
	}

	@Override
	public Theme getTheme()
	{
		return THEME;
	}
}