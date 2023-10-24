package serverutils.lib.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;

import org.lwjgl.input.Keyboard;

import serverutils.lib.client.GlStateManager;
import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.misc.MouseButton;

public class TextBox extends Widget {

    private boolean isFocused = false;
    public int charLimit = 250;
    public Color4I textColor = Icon.EMPTY;

    public String ghostText = "";
    private String text = "";
    private int lineScrollOffset;
    private int cursorPosition;
    private int selectionEnd;
    private boolean validText;

    public TextBox(Panel panel) {
        super(panel);
        setText("", false);
    }

    public final boolean isFocused() {
        return isFocused;
    }

    public final void setFocused(boolean v) {
        isFocused = v;
        validText = isValid(text);
        Keyboard.enableRepeatEvents(isFocused);
    }

    @Override
    public void onClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    public final String getText() {
        return text;
    }

    public String getSelectedText() {
        return text.substring(Math.min(cursorPosition, selectionEnd), Math.max(cursorPosition, selectionEnd));
    }

    public final void setText(String s, boolean triggerChange) {
        text = s;

        if (text.isEmpty()) {
            lineScrollOffset = 0;
            cursorPosition = 0;
            selectionEnd = 0;
        }

        validText = isValid(s);

        if (validText && triggerChange) {
            onTextChanged();
        }
    }

    public final void setText(String s) {
        setText(s, true);
    }

    public void setCursorPosition(int pos) {
        cursorPosition = pos;
        int i = text.length();
        cursorPosition = MathHelper.clamp_int(cursorPosition, 0, i);
        setSelectionPos(cursorPosition);
    }

    public void moveCursorBy(int num) {
        setCursorPosition(selectionEnd + num);
    }

    public void writeText(String textToWrite) {
        if (!textToWrite.isEmpty() && !allowInput()) {
            return;
        }

        String s = "";
        String s1 = ChatAllowedCharacters.filerAllowedCharacters(textToWrite);
        int i = Math.min(cursorPosition, selectionEnd);
        int j = Math.max(cursorPosition, selectionEnd);
        int k = charLimit - text.length() - (i - j);

        if (!text.isEmpty()) {
            s = s + text.substring(0, i);
        }

        int l;

        if (k < s1.length()) {
            s = s + s1.substring(0, k);
            l = k;
        } else {
            s = s + s1;
            l = s1.length();
        }

        if (!text.isEmpty() && j < text.length()) {
            s = s + text.substring(j);
        }

        setText(s);
        moveCursorBy(i - selectionEnd + l);
    }

    public void setSelectionPos(int position) {
        int i = text.length();

        if (position > i) {
            position = i;
        }

        if (position < 0) {
            position = 0;
        }

        selectionEnd = position;

        if (lineScrollOffset > i) {
            lineScrollOffset = i;
        }

        int j = width - 10;
        Theme theme = getGui().getTheme();
        String s = theme.trimStringToWidth(text.substring(lineScrollOffset), j);
        int k = s.length() + lineScrollOffset;

        if (position == lineScrollOffset) {
            lineScrollOffset -= theme.trimStringToWidthReverse(text, j).length();
        }

        if (position > k) {
            lineScrollOffset += position - k;
        } else if (position <= lineScrollOffset) {
            lineScrollOffset -= lineScrollOffset - position;
        }

        lineScrollOffset = MathHelper.clamp_int(lineScrollOffset, 0, i);
    }

    public int getNthWordFromCursor(int numWords) {
        return getNthWordFromPos(numWords, cursorPosition);
    }

    public int getNthWordFromPos(int n, int pos) {
        return getNthWordFromPosWS(n, pos, true);
    }

    public int getNthWordFromPosWS(int n, int pos, boolean skipWs) {
        int i = pos;
        boolean flag = n < 0;
        int j = Math.abs(n);

        for (int k = 0; k < j; ++k) {
            if (!flag) {
                int l = text.length();
                i = text.indexOf(32, i);

                if (i == -1) {
                    i = l;
                } else {
                    while (skipWs && i < l && text.charAt(i) == 32) {
                        ++i;
                    }
                }
            } else {
                while (skipWs && i > 0 && text.charAt(i - 1) == 32) {
                    --i;
                }

                while (i > 0 && text.charAt(i - 1) != 32) {
                    --i;
                }
            }
        }

        return i;
    }

    public boolean allowInput() {
        return true;
    }

    public void deleteWords(int num) {
        if (!text.isEmpty() && allowInput()) {
            if (selectionEnd != cursorPosition) {
                writeText("");
            } else {
                deleteFromCursor(getNthWordFromCursor(num) - cursorPosition);
            }
        }
    }

    public void deleteFromCursor(int num) {
        if (text.isEmpty() || !allowInput()) {
            return;
        }

        if (selectionEnd != cursorPosition) {
            writeText("");
        } else {
            boolean flag = num < 0;
            int i = flag ? cursorPosition + num : cursorPosition;
            int j = flag ? cursorPosition : cursorPosition + num;
            String s = "";

            if (i >= 0) {
                s = text.substring(0, i);
            }

            if (j < text.length()) {
                s = s + text.substring(j);
            }

            setText(s);

            if (flag) {
                moveCursorBy(num);
            }
        }
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        if (isMouseOver()) {
            setFocused(true);
            Keyboard.enableRepeatEvents(true);

            if (button.isLeft()) {
                if (isFocused) {
                    int i = getMouseX() - getX();
                    Theme theme = getGui().getTheme();
                    String s = theme.trimStringToWidth(text.substring(lineScrollOffset), width);
                    setCursorPosition(theme.trimStringToWidth(s, i).length() + lineScrollOffset);
                }
            } else if (button.isRight() && getText().length() > 0 && allowInput()) {
                setText("");
            }

            return true;
        } else {
            Keyboard.enableRepeatEvents(false);
            setFocused(false);
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, char keyChar) {
        if (!isFocused()) {
            return false;
        } else if (isKeyComboCtrlA(keyCode)) {
            setCursorPosition(text.length());
            setSelectionPos(0);
            return true;
        } else if (isKeyComboCtrlC(keyCode)) {
            setClipboardString(getSelectedText());
            return true;
        } else if (isKeyComboCtrlV(keyCode)) {
            writeText(getClipboardString());
            return true;
        } else if (isKeyComboCtrlX(keyCode)) {
            setClipboardString(getSelectedText());
            writeText("");
            return true;
        } else {
            switch (keyCode) {
                case Keyboard.KEY_BACK:
                    if (isCtrlKeyDown()) {
                        deleteWords(-1);
                    } else {
                        deleteFromCursor(-1);
                    }
                    return true;
                case Keyboard.KEY_HOME:
                    if (isShiftKeyDown()) {
                        setSelectionPos(0);
                    } else {
                        setCursorPosition(0);
                    }
                    return true;
                case Keyboard.KEY_LEFT:
                    if (isShiftKeyDown()) {
                        if (isCtrlKeyDown()) {
                            setSelectionPos(getNthWordFromPos(-1, selectionEnd));
                        } else {
                            setSelectionPos(selectionEnd - 1);
                        }
                    } else if (isCtrlKeyDown()) {
                        setCursorPosition(getNthWordFromCursor(-1));
                    } else {
                        moveCursorBy(-1);
                    }
                    return true;
                case Keyboard.KEY_RIGHT:
                    if (isShiftKeyDown()) {
                        if (isCtrlKeyDown()) {
                            setSelectionPos(getNthWordFromPos(1, selectionEnd));
                        } else {
                            setSelectionPos(selectionEnd + 1);
                        }
                    } else if (isCtrlKeyDown()) {
                        setCursorPosition(getNthWordFromCursor(1));
                    } else {
                        moveCursorBy(1);
                    }
                    return true;
                case Keyboard.KEY_END:
                    if (isShiftKeyDown()) {
                        setSelectionPos(text.length());
                    } else {
                        setCursorPosition(text.length());
                    }
                    return true;
                case Keyboard.KEY_DELETE:
                    if (isCtrlKeyDown()) {
                        deleteWords(1);
                    } else {
                        deleteFromCursor(1);
                    }
                    return true;
                case Keyboard.KEY_RETURN:
                    if (validText) {
                        setFocused(false);
                        onEnterPressed();
                    }
                    return true;
                case Keyboard.KEY_TAB:
                    if (validText) {
                        setFocused(false);
                        onTabPressed();
                    }
                    return true;
                default:
                    if (ChatAllowedCharacters.isAllowedCharacter(keyChar)) {
                        writeText(Character.toString(keyChar));
                        return true;
                    } else {
                        return false;
                    }
            }
        }
    }

    public void onTextChanged() {}

    public void onTabPressed() {}

    public void onEnterPressed() {}

    @Override
    public void draw(Theme theme, int x, int y, int w, int h) {
        drawTextBox(theme, x, y, w, h);
        boolean drawGhostText = !isFocused() && text.isEmpty() && !ghostText.isEmpty();
        String textToDraw = drawGhostText ? (EnumChatFormatting.ITALIC + ghostText) : text;
        GuiHelper.pushScissor(getScreen(), x, y, w, h);

        Color4I col = validText ? (textColor.isEmpty() ? theme.getContentColor(WidgetType.NORMAL) : textColor)
                .withAlpha(drawGhostText ? 120 : 255) : Color4I.RED;
        int j = cursorPosition - lineScrollOffset;
        int k = selectionEnd - lineScrollOffset;
        String s = theme.trimStringToWidth(textToDraw.substring(lineScrollOffset), w);
        int textX = x + 4;
        int textY = y + (h - 8) / 2;
        int textX1 = textX;

        if (k > s.length()) {
            k = s.length();
        }

        if (!s.isEmpty()) {
            String s1 = j > 0 && j <= s.length() ? s.substring(0, j) : s;
            textX1 = theme.drawString(s1, textX, textY, col, 0);
        }

        boolean drawCursor = cursorPosition < textToDraw.length() || textToDraw.length() >= charLimit;
        int cursorX = textX1;

        if (j <= 0 || j > s.length()) {
            cursorX = j > 0 ? textX + w : textX;
        } else if (drawCursor) {
            cursorX = textX1 - 1;
            // --textX1;
        }

        if (j > 0 && j < s.length()) {
            theme.drawString(s.substring(j), textX1, textY, col, 0);
        }

        if (j >= 0 && j <= s.length() && isFocused() && Minecraft.getSystemTime() % 1000L > 500L) {
            if (drawCursor) {
                col.draw(cursorX, textY - 1, 1, theme.getFontHeight() + 2);
            } else {
                col.draw(cursorX, textY + theme.getFontHeight() - 2, 5, 1);
            }
        }

        if (k != j) {
            int l1 = textX + theme.getStringWidth(s.substring(0, k));

            int startX = cursorX, startY = textY - 1, endX = l1 - 1, endY = textY + 1 + theme.getFontHeight();

            if (startX < endX) {
                int i = startX;
                startX = endX;
                endX = i;
            }

            if (startY < endY) {
                int j12 = startY;
                startY = endY;
                endY = j12;
            }

            if (endX > x + w) {
                endX = x + w;
            }

            if (startX > x + w) {
                startX = x + w;
            }

            Tessellator tessellator = Tessellator.instance;
            GlStateManager.color(0F, 0F, 255F, 255F);
            GlStateManager.disableTexture2D();
            GlStateManager.enableColorLogic();
            GlStateManager.colorLogicOp(GlStateManager.LogicOp.OR_REVERSE.opcode);
            tessellator.startDrawingQuads();
            tessellator.addVertex(startX, endY, 0);
            tessellator.addVertex(endX, endY, 0);
            tessellator.addVertex(endX, startY, 0);
            tessellator.addVertex(startX, startY, 0);
            tessellator.draw();
            GlStateManager.disableColorLogic();
            GlStateManager.enableTexture2D();
        }

        GuiHelper.popScissor(getScreen());
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    public void drawTextBox(Theme theme, int x, int y, int w, int h) {
        theme.drawTextBox(x, y, w, h);
    }

    public boolean isValid(String txt) {
        return true;
    }

    public final boolean isTextValid() {
        return validText;
    }
}
