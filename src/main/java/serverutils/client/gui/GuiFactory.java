package serverutils.client.gui;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiFactory;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;

public class GuiFactory implements SimpleGuiFactory {

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return GuiConfig.class;
    }

    public static class GuiConfig extends SimpleGuiConfig {

        public GuiConfig(GuiScreen parent) throws ConfigException {
            super(parent, ServerUtilities.MOD_ID, ServerUtilities.MOD_NAME, true, ServerUtilitiesConfig.class);
        }
    }
}
