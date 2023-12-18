package serverutils.client.gui;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;

public class GuiForgeConfig extends GuiConfig {

    public GuiForgeConfig(GuiScreen guiScreen) {
        super(
                Minecraft.getMinecraft().currentScreen,
                getConfigElements(),
                ServerUtilities.MOD_ID,
                false,
                false,
                getAbridgedConfigPath(ServerUtilities.MOD_ID + "/serverutilities.cfg"));
    }

    private static List<IConfigElement> getConfigElements() {
        final Configuration config = ServerUtilitiesConfig.config;
        return config.getCategoryNames().stream().filter(name -> name.indexOf('.') == -1)
                .map(name -> new ConfigElement(config.getCategory(name))).collect(Collectors.toList());
    }
}
