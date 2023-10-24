package serverutils.client.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import serverutils.ServerUtilities;
import serverutils.client.ServerUtilitiesClient;
import serverutils.client.ServerUtilitiesClientConfig;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.GuiIcons;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.WidgetType;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.gui.misc.GuiLoading;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.SidedUtils;
import serverutils.lib.util.misc.MouseButton;

public class GuiClientConfig extends GuiButtonListBase {

    private List<IConfigElement> configElement = new ConfigElement<>(
            ServerUtilitiesClientConfig.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements();

    private class GuiCustomConfig extends GuiConfig {

        public GuiCustomConfig(String title) {
            super(
                    Minecraft.getMinecraft().currentScreen,
                    configElement,
                    ServerUtilities.MOD_ID,
                    "serverutilities_client",
                    false,
                    false,
                    title,
                    getAbridgedConfigPath(ServerUtilitiesClient.CLIENT_FOLDER + "serverutilities.cfg"));
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

        panel.add(
                new SimpleTextButton(
                        panel,
                        ServerUtilities.MOD_NAME,
                        Icon.getIcon("serverutilities:textures/logo_small.png")) {

                    @Override
                    public void onClicked(MouseButton button) {
                        GuiHelper.playClickSound();
                        Minecraft.getMinecraft().displayGuiScreen(new GuiCustomConfig(getTitle()));
                    }
                });
    }

    @Override
    public void onClosed() {
        super.onClosed();
        SidebarButtonManager.INSTANCE.saveConfig();
    }
}
