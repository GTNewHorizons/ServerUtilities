package serverutils.lib.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.common.Loader;
import serverutils.lib.util.misc.MouseButton;

public class GuiWrapper extends GuiScreen implements IGuiWrapper {

    private GuiBase wrappedGui;
    private List<String> tempTextList = new ArrayList<>();

    public GuiWrapper(GuiBase g) {
        wrappedGui = g;
    }

    @Override
    public void initGui() {
        super.initGui();
        wrappedGui.initGui();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return wrappedGui.doesGuiPauseGame();
    }

    @Override
    protected final void mouseClicked(int mouseX, int mouseY, int button) {
        wrappedGui.updateMouseOver(mouseX, mouseY);

        if (button == MouseButton.BACK.id) {
            wrappedGui.onBack();
        } else {
            wrappedGui.mousePressed(MouseButton.get(button));
            super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        wrappedGui.updateMouseOver(mouseX, mouseY);
        wrappedGui.mouseReleased(MouseButton.get(button));
        super.mouseMovedOrUp(mouseX, mouseY, button);
    }

    @Override
    protected void keyTyped(char keyChar, int key) {
        if (!wrappedGui.keyPressed(key, keyChar)) {
            if (key == Keyboard.KEY_BACK) {
                wrappedGui.onBack();
            } else if (wrappedGui.onClosedByKey(key)) {
                wrappedGui.closeGui(false);
            } else if (Loader.isModLoaded("jei")) {
                Object object = WrappedIngredient.unwrap(wrappedGui.getIngredientUnderMouse());

                if (object != null) {
                    handleIngredientKey(key, object);
                }
            }
        }
    }

    private void handleIngredientKey(int key, Object object) {
        // TODO: this was a hack from serverutilities
        // ServerUtilitiesJEIIntegration.handleIngredientKey(key, object);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();

        if (scroll != 0) {
            wrappedGui.mouseScrolled(scroll);
        }
    }

    @Override
    public void handleKeyboardInput() {
        if (!(Keyboard.getEventKey() == 0 && Keyboard.getEventCharacter() >= ' ' || Keyboard.getEventKeyState())) {
            wrappedGui.keyReleased(Keyboard.getEventKey());
        } else {
            super.handleKeyboardInput();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (wrappedGui.fixUnicode) {
            GuiHelper.setFixUnicode(true);
        }

        wrappedGui.updateGui(mouseX, mouseY, partialTicks);
        drawDefaultBackground();
        GuiHelper.setupDrawing();
        int x = wrappedGui.getX();
        int y = wrappedGui.getY();
        int w = wrappedGui.width;
        int h = wrappedGui.height;
        Theme theme = wrappedGui.getTheme();
        wrappedGui.draw(theme, x, y, w, h);
        wrappedGui.drawForeground(theme, x, y, w, h);

        if (wrappedGui.contextMenu != null) {
            wrappedGui.contextMenu.addMouseOverText(tempTextList);
        } else {
            wrappedGui.addMouseOverText(tempTextList);
        }

        if (tempTextList.isEmpty()) {
            Object object = wrappedGui.getIngredientUnderMouse();

            if (object instanceof WrappedIngredient && ((WrappedIngredient) object).tooltip) {
                Object ingredient = WrappedIngredient.unwrap(object);

                if (ingredient instanceof ItemStack) {
                    renderToolTip((ItemStack) ingredient, mouseX, mouseY);
                }
            }
        } else {
            drawHoveringText(tempTextList, mouseX, Math.max(mouseY, 18), theme.getFont());
        }

        tempTextList.clear();

        if (wrappedGui.fixUnicode) {
            GuiHelper.setFixUnicode(false);
        }
    }

    @Override
    public void drawDefaultBackground() {
        if (wrappedGui.drawDefaultBackground()) {
            super.drawDefaultBackground();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        wrappedGui.tick();
    }

    @Override
    public GuiBase getGui() {
        return wrappedGui;
    }
}
