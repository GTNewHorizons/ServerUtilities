package serverutils.lib.gui;

import java.util.ArrayList;
import java.util.List;

import serverutils.lib.icon.Icon;
import serverutils.lib.util.misc.MouseButton;

public class ButtonContainer extends SimpleTextButton {

    private final List<Button> subButtons = new ArrayList<>();
    private int offsetX;

    public ButtonContainer(Panel panel, String txt, Icon icon) {
        super(panel, txt, icon);
    }

    public void addSubButton(Button button) {
        subButtons.add(button);
    }

    public void setXOffset(int v) {
        offsetX = v;
    }

    @Override
    public void setX(int v) {
        super.setX(v);
        adjustSubButtons();
    }

    @Override
    public void setY(int v) {
        super.setY(v);

        for (Button button : subButtons) {
            button.setY(v);
        }
    }

    @Override
    public void setWidth(int v) {
        super.setWidth(v);

        // super calls this in the constructor, so we need to check for null
        if (subButtons == null) return;

        adjustSubButtons();
    }

    private void adjustSubButtons() {
        int startX = width + offsetX;
        for (Button btn : subButtons) {
            btn.setX(startX - btn.width - 1);
            startX -= btn.width - 1;
        }
    }

    @Override
    public void setHeight(int v) {
        super.setHeight(v);

        if (subButtons == null) return;
        for (Button button : subButtons) {
            button.setHeight(v);
        }
    }

    @Override
    public void draw(Theme theme, int x, int y, int w, int h) {
        super.draw(theme, x, y, w, h);

        for (Button button : subButtons) {
            button.draw(theme, button.posX, y, button.width, button.height);
        }
    }

    @Override
    public void drawBackground(Theme theme, int x, int y, int w, int h) {
        super.drawBackground(theme, x, y, w, h);

        for (Button button : subButtons) {
            button.drawBackground(theme, button.posX, y, button.width, button.height);
        }
    }

    @Override
    public void drawIcon(Theme theme, int x, int y, int w, int h) {
        for (Button button : subButtons) {
            button.drawIcon(theme, button.posX, y, button.width, button.height);
        }
    }

    @Override
    public void updateMouseOver(int mouseX, int mouseY) {
        for (Button subButton : subButtons) {
            subButton.updateMouseOver(mouseX, mouseY);
        }
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        for (Button subButton : subButtons) {
            if (subButton.mousePressed(button)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onClicked(MouseButton button) {

    }
}
