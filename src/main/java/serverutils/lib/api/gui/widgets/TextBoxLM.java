package serverutils.lib.api.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.gui.IGuiLM;

@SideOnly(Side.CLIENT)
public class TextBoxLM extends WidgetLM {

    public boolean isSelected = false;
    private String text = "";
    public int charLimit = -1;
    public int textRenderX = 4, textRenderY = 4;
    public int textColor = 0xFFFFFFFF;
    private boolean isValid = true;

    public TextBoxLM(IGuiLM g, int x, int y, int w, int h) {
        super(g, x, y, w, h);
        text = getText();
    }

    public void mousePressed(int b) {
        if (charLimit == 0) return;

        if (mouseOver()) {
            isSelected = true;
            Keyboard.enableRepeatEvents(true);

            if (b == 1 && getText().length() > 0) {
                clear();
                textChanged();
            }
        } else {
            Keyboard.enableRepeatEvents(false);
            isSelected = false;
        }
    }

    public boolean keyPressed(int key, char keyChar) {
        if (charLimit == 0) return false;

        if (isSelected) {
            if (key == Keyboard.KEY_BACK) {
                text = getText();
                if (text.length() > 0) {
                    if (GuiScreen.isCtrlKeyDown()) clear();
                    else setText(text.substring(0, text.length() - 1));
                    textChanged();
                }
            } else if (key == Keyboard.KEY_ESCAPE) {
                isSelected = false;
            } else if (key == Keyboard.KEY_TAB) {
                if (isValid()) {
                    tabPressed();
                    isSelected = false;
                }
            } else if (key == Keyboard.KEY_RETURN) {
                if (isValid()) {
                    returnPressed();
                    isSelected = false;
                }
            } else {
                text = getText();
                if ((charLimit == -1 || text.length() + 1 <= charLimit)
                        && ChatAllowedCharacters.isAllowedCharacter(keyChar)) {
                    setText(text + keyChar);
                    textChanged();
                }
            }

            return true;
        }

        return false;
    }

    public void textChanged() {}

    public void tabPressed() {}

    public void returnPressed() {}

    public void clear() {
        setText("");
    }

    public void setText(String s) {
        text = s == null ? "" : s;
    }

    public String getText() {
        return text;
    }

    public void renderWidget() {
        String s = getText();

        String ns = s;

        if (isSelected && Minecraft.getSystemTime() % 1000L > 500L) ns += '_';

        if (ns.length() > 0) {
            int col = textColor;
            if (!isValid()) col = 0xFFFF0000;

            if (textRenderX == -1) gui.getFontRenderer().drawString(
                    ns,
                    getAX() + textRenderX - (gui.getFontRenderer().getStringWidth(s) / 2) + width / 2,
                    getAY() + textRenderY,
                    col);
            else gui.getFontRenderer().drawString(ns, getAX() + textRenderX, getAY() + textRenderY, col);
        }
    }

    public boolean isValid() {
        return true;
    }
}
