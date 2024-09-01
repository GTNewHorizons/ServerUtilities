package serverutils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.util.StatCollector;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;

import serverutils.ServerUtilities;
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
import serverutils.lib.icon.ItemIcon;
import serverutils.lib.util.SidedUtils;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageCommandsRequest;

public class GuiClientConfig extends GuiButtonListBase {

    private class GuiCustomConfig extends SimpleGuiConfig {

        public GuiCustomConfig(String title) throws ConfigException {
            super(
                    Minecraft.getMinecraft().currentScreen,
                    ServerUtilities.MOD_ID,
                    title,
                    true,
                    ServerUtilitiesClientConfig.class);

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
                        StatCollector.translateToLocal("serverutilities_client"),
                        Icon.getIcon("serverutilities:textures/logo_small.png")) {

                    @Override
                    public void onClicked(MouseButton button) {
                        GuiHelper.playClickSound();
                        try {
                            Minecraft.getMinecraft().displayGuiScreen(new GuiCustomConfig(getTitle()));
                        } catch (ConfigException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        panel.add(
                new SimpleTextButton(
                        panel,
                        StatCollector.translateToLocal("serverutilities.command_overview"),
                        ItemIcon.getItemIcon(Items.compass)) {

                    @Override
                    public void onClicked(MouseButton button) {
                        GuiHelper.playClickSound();
                        new GuiLoading().openGui();
                        new MessageCommandsRequest().sendToServer();
                    }

                    @Override
                    public WidgetType getWidgetType() {
                        return SidedUtils.isModLoadedOnServer(ServerUtilities.MOD_ID) ? super.getWidgetType()
                                : WidgetType.DISABLED;
                    }
                });
    }

    @Override
    public void onClosed() {
        super.onClosed();
        SidebarButtonManager.INSTANCE.saveConfig();
    }
}
