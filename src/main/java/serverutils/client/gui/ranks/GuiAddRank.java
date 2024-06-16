package serverutils.client.gui.ranks;

import java.util.Collections;

import net.minecraft.util.StatCollector;

import serverutils.lib.client.ClientUtils;
import serverutils.lib.gui.Button;
import serverutils.lib.gui.GuiBase;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.SimpleTextButton;
import serverutils.lib.gui.TextBox;
import serverutils.lib.icon.Icon;
import serverutils.lib.util.StringUtils;
import serverutils.lib.util.misc.MouseButton;
import serverutils.net.MessageRankUpdateRequest;

public class GuiAddRank extends GuiBase {

    private final Button buttonAccept, buttonCancel;
    private final TextBox textBoxId;

    public GuiAddRank() {
        setSize(162, 62);

        int bwidth = width / 2 - 10;
        buttonAccept = new SimpleTextButton(this, StatCollector.translateToLocal("gui.accept"), Icon.EMPTY) {

            @Override
            public void onClicked(MouseButton button) {
                GuiHelper.playClickSound();
                String text = textBoxId.getText();
                if (!text.isEmpty()) {
                    getGui().closeGui(true);
                    ClientUtils.execClientCommand("/ranks create " + text);
                    GuiRanks.ranks.put(text, new RankInst(text));
                    new MessageRankUpdateRequest(Collections.singletonList(text)).sendToServer();
                }
            }

            @Override
            public boolean renderTitleInCenter() {
                return true;
            }
        };

        buttonAccept.setPosAndSize(width - bwidth - 9, height - 24, bwidth, 16);

        buttonCancel = new SimpleTextButton(this, StatCollector.translateToLocal("gui.cancel"), Icon.EMPTY) {

            @Override
            public void onClicked(MouseButton button) {
                GuiHelper.playClickSound();
                getGui().closeGui(true);
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

            @Override
            public void onEnterPressed() {
                buttonAccept.onClicked(MouseButton.LEFT);
            }
        };

        textBoxId.setPosAndSize(8, 8, width - 16, 16);
        textBoxId.writeText("");
        textBoxId.ghostText = "Enter Rank Name";
        textBoxId.setFocused(true);

    }

    @Override
    public void addWidgets() {
        add(buttonAccept);
        add(buttonCancel);
        add(textBoxId);
    }
}
