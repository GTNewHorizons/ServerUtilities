package serverutils.lib.gui.misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;

import serverutils.lib.client.ClientUtils;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.ConfigValueInstance;
import serverutils.lib.config.IConfigCallback;
import serverutils.lib.gui.Button;
import serverutils.lib.gui.GuiBase;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.PanelScrollBar;
import serverutils.lib.gui.SimpleButton;
import serverutils.lib.gui.Theme;
import serverutils.lib.gui.Widget;
import serverutils.lib.gui.WidgetLayout;
import serverutils.lib.gui.WidgetType;
import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.MutableColor4I;
import serverutils.lib.io.Bits;
import serverutils.lib.util.misc.MouseButton;

public class GuiEditConfig extends GuiBase {

    public static final Color4I COLOR_BACKGROUND = Color4I.rgba(0x99333333);
    public static final Comparator<ConfigValueInstance> COMPARATOR = (o1, o2) -> {
        int i = o1.getGroup().getPath().compareToIgnoreCase(o2.getGroup().getPath());

        if (i == 0) {
            i = Integer.compare(o1.getOrder(), o2.getOrder());
        }

        if (i == 0) {
            i = o1.getDisplayName().getUnformattedText().compareToIgnoreCase(o2.getDisplayName().getUnformattedText());
        }

        return i;
    };

    public static Theme THEME = new Theme() {

        @Override
        public void drawScrollBarBackground(int x, int y, int w, int h, WidgetType type) {
            Color4I.BLACK.withAlpha(70).draw(x, y, w, h);
        }

        @Override
        public void drawScrollBar(int x, int y, int w, int h, WidgetType type, boolean vertical) {
            getContentColor(WidgetType.NORMAL).withAlpha(100).withBorder(Color4I.GRAY.withAlpha(100), false)
                    .draw(x, y, w, h);
        }
    };

    public static class ButtonConfigGroup extends Button {

        public final ConfigGroup group;
        public String title, info;
        public boolean collapsed = false;

        public ButtonConfigGroup(Panel panel, ConfigGroup g) {
            super(panel);
            setHeight(12);
            group = g;

            if (group.parent != null) {
                List<ConfigGroup> groups = new ArrayList<>();

                do {
                    groups.add(g);
                    g = g.parent;
                } while (g != null);

                groups.remove(groups.size() - 1);

                StringBuilder builder = new StringBuilder();

                for (int i = groups.size() - 1; i >= 0; i--) {
                    builder.append(groups.get(i).getDisplayName().getFormattedText());

                    if (i != 0) {
                        builder.append(" > ");
                    }
                }

                title = builder.toString();
            } else {
                title = StatCollector.translateToLocal("stat.generalButton");
            }

            String infoKey = group.getPath() + ".info";
            info = StatCollector.canTranslate(infoKey) ? StatCollector.translateToLocal(infoKey) : "";
            setCollapsed(collapsed);
        }

        public void setCollapsed(boolean v) {
            collapsed = v;
            setTitle(
                    (collapsed ? (EnumChatFormatting.RED + "[-] ") : (EnumChatFormatting.GREEN + "[v] "))
                            + EnumChatFormatting.RESET
                            + title);
        }

        @Override
        public void draw(Theme theme, int x, int y, int w, int h) {
            COLOR_BACKGROUND.draw(x, y, w, h);
            theme.drawString(getTitle(), x + 2, y + 2);
            GlStateManager.color(1F, 1F, 1F, 1F);

            if (isMouseOver()) {
                Color4I.WHITE.withAlpha(33).draw(x, y, w, h);
            }
        }

        @Override
        public void addMouseOverText(List<String> list) {
            if (!info.isEmpty()) {
                list.add(info);
            }
        }

        @Override
        public void onClicked(MouseButton button) {
            setCollapsed(!collapsed);
            getGui().refreshWidgets();
        }
    }

    protected class ButtonConfigEntry extends Button {

        public final ButtonConfigGroup group;
        public final ConfigValueInstance inst;
        public String keyText;
        protected String valueString = null;

        public ButtonConfigEntry(Panel panel, @Nullable ButtonConfigGroup g, ConfigValueInstance i) {
            super(panel);
            setHeight(12);
            group = g;
            inst = i;
            addKeyText();
        }

        protected void addKeyText() {
            if (!inst.getCanEdit()) {
                IChatComponent c = inst.getDisplayName().createCopy();
                c.getChatStyle().setColor(EnumChatFormatting.GRAY);
                keyText = c.getFormattedText();
            } else {
                keyText = inst.getDisplayName().getFormattedText();
            }
        }

        public String getValueString() {
            if (valueString == null) {
                valueString = inst.getValue().getStringForGUI().getFormattedText();
            }

            return valueString;
        }

        @Override
        public void draw(Theme theme, int x, int y, int w, int h) {
            boolean mouseOver = getMouseY() >= 20 && isMouseOver();

            if (mouseOver) {
                Color4I.WHITE.withAlpha(33).draw(x, y, w, h);
            }

            theme.drawString(keyText, x + 4, y + 2, Bits.setFlag(0, Theme.SHADOW, mouseOver));
            GlStateManager.color(1F, 1F, 1F, 1F);

            String s = getValueString();
            int slen = theme.getStringWidth(s);

            if (slen > 150) {
                s = theme.trimStringToWidth(s, 150) + "...";
                slen = 152;
            }

            MutableColor4I textCol = inst.getValue().getColor().mutable();
            textCol.setAlpha(255);

            if (mouseOver) {
                textCol.addBrightness(60);

                if (getMouseX() > x + w - slen - 9) {
                    Color4I.WHITE.withAlpha(33).draw(x + w - slen - 8, y, slen + 8, h);
                }
            }

            theme.drawString(s, getGui().width - (slen + 20), y + 2, textCol, 0);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }

        @Override
        public void onClicked(MouseButton button) {
            if (getMouseY() >= 20) {
                GuiHelper.playClickSound();
                inst.getValue().onClicked(GuiEditConfig.this, inst, button, () -> valueString = null);
            }
        }

        @Override
        public void addMouseOverText(List<String> list) {
            if (getMouseY() > 18) {
                list.add(EnumChatFormatting.UNDERLINE + keyText);
                addDescriptionText(list);
                list.add("");
                inst.getValue().addInfo(inst, list);
            }
        }

        protected void addDescriptionText(List<String> list) {
            IChatComponent infoText = inst.getInfo();
            EnumChatFormatting color = EnumChatFormatting.GRAY;

            if (!inst.getCanEdit()) {
                infoText = new ChatComponentText(ClientUtils.getDisabledTip());
                color = EnumChatFormatting.RED;
            }

            if (!(infoText instanceof ChatComponentTranslation component)
                    || StatCollector.canTranslate(component.getKey())) {
                for (String s : infoText.getFormattedText().split("\n")) {
                    list.add(color.toString() + EnumChatFormatting.ITALIC + s);
                }
            }
        }
    }

    protected final ConfigGroup group;
    protected final ConfigGroup originalGroup;
    protected final IConfigCallback callback;

    protected final String title;
    protected final List<Widget> configEntryButtons;
    protected final Panel configPanel;
    protected final Button buttonAccept, buttonCancel, buttonCollapseAll, buttonExpandAll;
    protected final PanelScrollBar scroll;
    protected int shouldClose = 0;
    protected int groupSize = 0;

    public GuiEditConfig(ConfigGroup g, @Nullable IConfigCallback c) {
        group = g;
        originalGroup = group.copy();
        callback = c;

        IChatComponent title0 = g.getDisplayName().createCopy();
        title0.getChatStyle().setBold(true);
        title = title0.getFormattedText();

        configEntryButtons = new ArrayList<>();

        configPanel = new Panel(this) {

            @Override
            public void addWidgets() {
                for (Widget w : configEntryButtons) {
                    if (!(w instanceof ButtonConfigEntry entry) || entry.group == null || !entry.group.collapsed) {
                        add(w);
                    }
                }
            }

            @Override
            public void alignWidgets() {
                for (Widget w : widgets) {
                    w.setWidth(width - 16);
                }

                scroll.setMaxValue(align(WidgetLayout.VERTICAL));
            }
        };

        addConfigEntryButtons(collectAllConfigValues(group));

        scroll = new PanelScrollBar(this, configPanel);

        buttonAccept = new SimpleButton(
                this,
                StatCollector.translateToLocal("gui.close"),
                GuiIcons.ACCEPT,
                (widget, button) -> {
                    shouldClose = 1;
                    widget.getGui().closeGui();
                });

        buttonCancel = new SimpleButton(
                this,
                StatCollector.translateToLocal("gui.cancel"),
                GuiIcons.CANCEL,
                (widget, button) -> {
                    shouldClose = 2;
                    widget.getGui().closeGui();
                });

        buttonExpandAll = new SimpleButton(
                this,
                StatCollector.translateToLocal("gui.expand_all"),
                GuiIcons.ADD,
                (widget, button) -> {
                    for (Widget w : configEntryButtons) {
                        if (w instanceof ButtonConfigGroup configGroup) {
                            configGroup.setCollapsed(false);
                        }
                    }

                    scroll.setValue(0);
                    widget.getGui().refreshWidgets();
                });

        buttonCollapseAll = new SimpleButton(
                this,
                StatCollector.translateToLocal("gui.collapse_all"),
                GuiIcons.REMOVE,
                (widget, button) -> {
                    for (Widget w : configEntryButtons) {
                        if (w instanceof ButtonConfigGroup configGroup) {
                            configGroup.setCollapsed(true);
                        }
                    }

                    scroll.setValue(0);
                    widget.getGui().refreshWidgets();
                });
    }

    protected List<ConfigValueInstance> collectAllConfigValues(ConfigGroup group) {
        List<ConfigValueInstance> list = new ArrayList<>();
        for (ConfigValueInstance instance : group.getValues()) {
            if (!instance.getHidden()) {
                list.add(instance);
            }
        }

        for (ConfigGroup group1 : group.getGroups()) {
            list.addAll(collectAllConfigValues(group1));
        }

        return list;
    }

    @Override
    public boolean onInit() {
        for (Widget widget : configEntryButtons) {
            if (widget instanceof ButtonConfigEntry entry) {
                entry.valueString = null;
            }
        }

        return setFullscreen();
    }

    @Override
    public void addWidgets() {
        add(buttonAccept);
        add(buttonCancel);

        if (groupSize > 1) {
            add(buttonExpandAll);
            add(buttonCollapseAll);
        }

        add(configPanel);
        add(scroll);
    }

    @Override
    public void alignWidgets() {
        configPanel.setPosAndSize(0, 20, width, height - 20);
        configPanel.alignWidgets();
        scroll.setPosAndSize(width - 16, 20, 16, height - 20);

        buttonAccept.setPos(width - 18, 2);
        buttonCancel.setPos(width - 38, 2);

        if (groupSize > 1) {
            buttonExpandAll.setPos(width - 58, 2);
            buttonCollapseAll.setPos(width - 78, 2);
        }
    }

    @Override
    public void onClosed() {
        super.onClosed();

        if (shouldClose != 1) {
            group.deserializeNBT(originalGroup.serializeNBT());
        }

        if (callback == null) return;
        callback.onConfigSaved(group, Minecraft.getMinecraft().thePlayer);
    }

    @Override
    public boolean onClosedByKey(int key) {
        if (super.onClosedByKey(key)) {
            shouldClose = 1;
            closeGui();
        }

        return false;
    }

    protected void addConfigEntryButtons(List<ConfigValueInstance> list) {
        if (!list.isEmpty()) {
            list.sort(COMPARATOR);

            ButtonConfigGroup group = null;

            for (ConfigValueInstance instance : list) {
                if (instance.getExcluded()) continue;

                if (group == null || !group.group.equals(instance.getGroup())) {
                    group = new ButtonConfigGroup(configPanel, instance.getGroup());
                    configEntryButtons.add(group);
                    groupSize++;
                }
                configEntryButtons.add(getEntryButton(configPanel, group, instance));
            }

            if (groupSize == 1) {
                configEntryButtons.remove(group);
            }
        }
    }

    protected Widget getEntryButton(Panel panel, @Nullable ButtonConfigGroup group, ConfigValueInstance instance) {
        return new ButtonConfigEntry(panel, group, instance);
    }

    @Override
    public void drawBackground(Theme theme, int x, int y, int w, int h) {
        COLOR_BACKGROUND.draw(0, 0, w, 20);
        theme.drawString(getTitle(), 6, 6, Theme.SHADOW);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Theme getTheme() {
        return THEME;
    }
}
