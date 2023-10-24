package serverutils.lib.gui;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import serverutils.lib.util.misc.MouseButton;

public class Widget implements IGuiWrapper {

    public Panel parent;
    public int posX, posY, width, height;
    protected boolean isMouseOver;

    public Widget(Panel p) {
        parent = p;
    }

    @Override
    public GuiBase getGui() {
        return parent.getGui();
    }

    public void setX(int v) {
        posX = v;
    }

    public void setY(int v) {
        posY = v;
    }

    public void setWidth(int v) {
        width = Math.max(v, 0);
    }

    public void setHeight(int v) {
        height = Math.max(v, 0);
    }

    public final void setPos(int x, int y) {
        setX(x);
        setY(y);
    }

    public final void setSize(int w, int h) {
        setWidth(w);
        setHeight(h);
    }

    public final Widget setPosAndSize(int x, int y, int w, int h) {
        setX(x);
        setY(y);
        setWidth(w);
        setHeight(h);
        return this;
    }

    public int getX() {
        return parent.getX() + posX;
    }

    public int getY() {
        return parent.getY() + posY;
    }

    public boolean collidesWith(int x, int y, int w, int h) {
        int ay = getY();
        if (ay >= y + h || ay + height <= y) {
            return false;
        }

        int ax = getX();
        return ax < x + w && ax + width > x;
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean shouldDraw() {
        return true;
    }

    public String getTitle() {
        return "";
    }

    public WidgetType getWidgetType() {
        return WidgetType.mouseOver(isMouseOver());
    }

    public void addMouseOverText(List<String> list) {
        String title = getTitle();

        if (!title.isEmpty()) {
            list.add(title);
        }
    }

    public final boolean isMouseOver() {
        return isMouseOver;
    }

    public boolean checkMouseOver(int mouseX, int mouseY) {
        if (parent == null) {
            return true;
        } else if (!parent.isMouseOver()) {
            return false;
        }

        int ax = getX();
        int ay = getY();
        return mouseX >= ax && mouseY >= ay && mouseX < ax + width && mouseY < ay + height;
    }

    public void updateMouseOver(int mouseX, int mouseY) {
        isMouseOver = checkMouseOver(mouseX, mouseY);
    }

    public boolean shouldAddMouseOverText() {
        return isEnabled() && isMouseOver();
    }

    public void draw(Theme theme, int x, int y, int w, int h) {}

    public boolean mousePressed(MouseButton button) {
        return false;
    }

    public void mouseReleased(MouseButton button) {}

    public boolean mouseScrolled(int scroll) {
        return false;
    }

    public boolean keyPressed(int key, char keyChar) {
        return false;
    }

    public void keyReleased(int key) {}

    public ScaledResolution getScreen() {
        return parent.getScreen();
    }

    public int getMouseX() {
        return parent.getMouseX();
    }

    public int getMouseY() {
        return parent.getMouseY();
    }

    public float getPartialTicks() {
        return parent.getPartialTicks();
    }

    public boolean handleClick(String scheme, String path) {
        return parent.handleClick(scheme, path);
    }

    public final boolean handleClick(String click) {
        int index = click.indexOf(':');

        if (index == -1) {
            return handleClick("", click);
        }

        return handleClick(click.substring(0, index), click.substring(index + 1));
    }

    public void onClosed() {}

    @Nullable
    public Object getIngredientUnderMouse() {
        return null;
    }

    public boolean isGhostIngredientTarget(Object ingredient) {
        return false;
    }

    public void acceptGhostIngredient(Object ingredient) {}

    public static boolean isMouseButtonDown(MouseButton button) {
        return Mouse.isButtonDown(button.id);
    }

    public static boolean isKeyDown(int key) {
        return Keyboard.isKeyDown(key);
    }

    public static String getClipboardString() {
        return GuiScreen.getClipboardString();
    }

    public static void setClipboardString(String string) {
        GuiScreen.setClipboardString(string);
    }

    public static boolean isShiftKeyDown() {
        return GuiScreen.isShiftKeyDown();
    }

    public static boolean isCtrlKeyDown() {
        return GuiScreen.isCtrlKeyDown();
    }

    public static boolean isAltKeyDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
    }

    public static boolean isKeyComboCtrlX(int key) {
        return isCtrlKeyDown() && Keyboard.isKeyDown(Keyboard.KEY_X);
    }

    public static boolean isKeyComboCtrlV(int key) {
        return isCtrlKeyDown() && Keyboard.isKeyDown(Keyboard.KEY_V);
    }

    public static boolean isKeyComboCtrlC(int key) {
        return isCtrlKeyDown() && Keyboard.isKeyDown(Keyboard.KEY_C);
    }

    public static boolean isKeyComboCtrlA(int key) {
        return isCtrlKeyDown() && Keyboard.isKeyDown(Keyboard.KEY_A);
    }

    public void tick() {}

    public String toString() {
        String s = getClass().getSimpleName();

        if (s.isEmpty()) {
            s = getClass().getSuperclass().getSimpleName();
        }

        return s;
    }
}
