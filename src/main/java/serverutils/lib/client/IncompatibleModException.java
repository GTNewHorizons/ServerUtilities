package serverutils.lib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;

import cpw.mods.fml.client.CustomModLoadingErrorDisplayException;

public class IncompatibleModException extends CustomModLoadingErrorDisplayException {

    private static final String ERROR = "FTBUtilities/FTBLibrary detected during load.";
    private static final String RESTART = "Please remove them and restart the game.";

    public IncompatibleModException() {
        super(ERROR, new RuntimeException());
    }

    @Override
    public void initGui(GuiErrorScreen errorScreen, FontRenderer fontRenderer) {
        Minecraft.getMinecraft().displayGuiScreen(errorGui);
    }

    @Override
    public void drawScreen(GuiErrorScreen errorScreen, FontRenderer fontRenderer, int mouseRelX, int mouseRelY,
            float tickTime) {}

    final GuiScreen errorGui = new GuiErrorScreen(ERROR, RESTART) {

        @Override
        public void initGui() {
            this.buttonList.add(new GuiButton(0, this.width / 2 - 100, 140, "Close Game"));
        }

        @Override
        public void onGuiClosed() {
            Minecraft.getMinecraft().shutdown();
        }
    };
}
