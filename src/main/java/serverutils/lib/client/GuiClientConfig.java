package serverutils.lib.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import serverutils.lib.lib.client.ClientUtils;
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
import serverutils.mod.client.ServerUtilitiesClientConfig;

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
                    getAbridgedConfigPath("/server utilities/client/serverutilities.cfg"));
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
