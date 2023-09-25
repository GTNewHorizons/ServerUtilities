package serverutils.lib.client.teamsgui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import serverutils.lib.lib.EnumTeamColor;
import serverutils.lib.lib.client.ClientUtils;
import serverutils.lib.lib.data.ForgeTeam;
import serverutils.lib.lib.gui.Button;
import serverutils.lib.lib.gui.GuiBase;
import serverutils.lib.lib.gui.GuiHelper;
import serverutils.lib.lib.gui.SimpleTextButton;
import serverutils.lib.lib.gui.TextBox;
import serverutils.lib.lib.gui.Theme;
import serverutils.lib.lib.icon.Icon;
import serverutils.lib.lib.math.MathUtils;
import serverutils.lib.lib.util.StringUtils;
import serverutils.lib.lib.util.misc.MouseButton;

public class GuiCreateTeam extends GuiBase {

    private EnumTeamColor color;
    private final Button buttonAccept, buttonCancel;
    private final List<Button> colorButtons;
    private final TextBox textBoxId;

    public GuiCreateTeam() {
        setSize(162, 118);
        color = EnumTeamColor.NAME_MAP.getRandom(MathUtils.RAND);

        int bwidth = width / 2 - 10;
        buttonAccept = new SimpleTextButton(this, I18n.format("gui.accept"), Icon.EMPTY) {

            @Override
            public void onClicked(MouseButton button) {
                GuiHelper.playClickSound();

                if (!textBoxId.getText().isEmpty()) {
                    getGui().closeGui(false);
                    ClientUtils.execClientCommand("/team create " + textBoxId.getText() + " " + color.getName());
                }
            }

            @Override
            public boolean renderTitleInCenter() {
                return true;
            }
        };

        buttonAccept.setPosAndSize(width - bwidth - 9, height - 24, bwidth, 16);

        buttonCancel = new SimpleTextButton(this, I18n.format("gui.cancel"), Icon.EMPTY) {

            @Override
            public void onClicked(MouseButton button) {
                GuiHelper.playClickSound();
                getGui().closeGui();
            }

            @Override
            public boolean renderTitleInCenter() {
                return true;
            }
        };

        buttonCancel.setPosAndSize(8, height - 24, bwidth, 16);

        textBoxId = new TextBox(this) {

            @Override
            public void onTextChanged() {
                setText(StringUtils.getID(getText(), StringUtils.FLAG_ID_DEFAULTS), false);
            }
        };

        textBoxId.setPosAndSize(8, 8, width - 16, 16);
        textBoxId.writeText(Minecraft.getMinecraft().thePlayer.getGameProfile().getName().toLowerCase());
        textBoxId.ghostText = "Enter ID"; // LANG
        textBoxId.textColor = color.getColor();
        textBoxId.setFocused(true);
        textBoxId.charLimit = ForgeTeam.MAX_TEAM_ID_LENGTH;

        colorButtons = new ArrayList<>();
        int i = 0;

        for (EnumTeamColor col : EnumTeamColor.NAME_MAP) {
            Button b = new Button(this) {

                @Override
                public void onClicked(MouseButton button) {
                    color = col;
                    textBoxId.textColor = color.getColor();
                }

                @Override
                public void drawBackground(Theme theme, int x, int y, int w, int h) {
                    theme.drawPanelBackground(x, y, w, h);
                    col.getColor().withAlpha(color == col || isMouseOver() ? 200 : 100).draw(x, y, w, h);
                }
            };

            b.setPosAndSize(8 + (i % 5) * 30, 32 + (i / 5) * 30, 25, 25);
            b.setTitle(col.getEnumChatFormatting() + I18n.format(col.getLangKey()));
            colorButtons.add(b);
            i++;
        }
    }

    @Override
    public void addWidgets() {
        add(buttonAccept);
        add(buttonCancel);
        addAll(colorButtons);
        add(textBoxId);
    }
}
