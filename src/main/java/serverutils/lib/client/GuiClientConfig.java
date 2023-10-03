package serverutils.lib.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiMessageDialog;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.Event;
import serverutils.lib.lib.client.ClientUtils;
import serverutils.lib.lib.gui.Button;
import serverutils.lib.lib.gui.GuiHelper;
import serverutils.lib.lib.gui.GuiIcons;
import serverutils.lib.lib.gui.Panel;
import serverutils.lib.lib.gui.SimpleTextButton;
import serverutils.lib.lib.gui.WidgetType;
import serverutils.lib.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.lib.gui.misc.GuiLoading;
import serverutils.lib.lib.icon.Icon;
import serverutils.lib.lib.util.SidedUtils;
import serverutils.lib.lib.util.misc.MouseButton;
import serverutils.mod.ServerUtilities;
import serverutils.mod.client.ServerUtilitiesClient;

public class GuiClientConfig extends GuiButtonListBase {

    private class GuiCustomConfig extends GuiConfig {

        public GuiCustomConfig(String modid, String title) {
            super(Minecraft.getMinecraft().currentScreen, new ArrayList<>(), modid, false, false, title);
        }

        @Override
        protected void actionPerformed(GuiButton button) {
            if (button.id == 2000) {
                boolean flag = true;
                try {
                    if ((configID != null || !(parentScreen instanceof GuiConfig)) && entryList.hasChangedEntry(true)) {
                        boolean requiresMcRestart = entryList.saveConfigElements();

                        ConfigChangedEvent event = new ConfigChangedEvent.OnConfigChangedEvent(
                                modID,
                                configID,
                                isWorldRunning,
                                requiresMcRestart);
                        MinecraftForge.EVENT_BUS.post(event);
                        if (!event.getResult().equals(Event.Result.DENY)) {
                            MinecraftForge.EVENT_BUS.post(
                                    new ConfigChangedEvent.PostConfigChangedEvent(
                                            modID,
                                            configID,
                                            isWorldRunning,
                                            requiresMcRestart));
                        }

                        if (requiresMcRestart) {
                            flag = false;
                            mc.displayGuiScreen(
                                    new GuiMessageDialog(
                                            parentScreen,
                                            "fml.configgui.gameRestartTitle",
                                            new ChatComponentText(I18n.format("fml.configgui.gameRestartRequired")),
                                            "fml.configgui.confirmRestartMessage"));
                        }

                        if (parentScreen instanceof GuiConfig) {
                            ((GuiConfig) parentScreen).needsRefresh = true;
                        }
                    }
                } catch (Throwable e) {
                    FMLLog.log(Level.ERROR, "Error performing GuiConfig action:", e);
                }

                if (flag) {
                    mc.displayGuiScreen(parentScreen);
                }
            } else {
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

    public GuiClientConfig() {
        setTitle(I18n.format("sidebar_button.serverutilities.settings"));
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
                return SidedUtils.isModLoadedOnServer(ServerUtilities.MOD_ID) ? super.getWidgetType()
                        : WidgetType.DISABLED;
            }
        });

        panel.add(
                new SimpleTextButton(
                        panel,
                        I18n.format("sidebar_button"),
                        Icon.getIcon("serverutilities:textures/gui/teams.png")) {

                    @Override
                    public void onClicked(MouseButton button) {
                        GuiHelper.playClickSound();
                        new GuiSidebarButtonConfig().openGui();
                    }
                });

        List<Button> buttons = new ArrayList<>();

        for (ClientConfig config : ServerUtilitiesClient.CLIENT_CONFIG_MAP.values()) {
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
