package serverutils.invsee;

import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import serverutils.invsee.inventories.IModdedInventory;
import serverutils.invsee.inventories.InvSeeInventories;
import serverutils.lib.gui.Button;
import serverutils.lib.gui.GuiBase;
import serverutils.lib.gui.GuiContainerWrapper;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.SimpleButton;
import serverutils.lib.gui.TextField;
import serverutils.lib.gui.Theme;
import serverutils.lib.gui.Widget;
import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.Icon;
import serverutils.lib.icon.PlayerHeadIcon;
import serverutils.lib.util.StringUtils;
import serverutils.net.MessageInvseeSwitch;

public class GuiInvseeContainer extends GuiBase {

    private static final Icon BUTTON_BACKGROUND = Color4I.GRAY.withBorder(Color4I.DARK_GRAY, true);
    private final Map<InvSeeInventories, IInventory> inventories;
    private final InvseeContainer container;
    private final String playerName;
    private final PlayerHeadIcon playerIcon;
    private final GuiContainerWrapper wrapper;
    private int topY;
    private String inventoryName;
    private TextField textField;

    public GuiInvseeContainer(Map<InvSeeInventories, IInventory> inventories, String playerName, String playerId) {
        this.inventories = inventories;
        this.container = new InvseeContainer(inventories, Minecraft.getMinecraft().thePlayer, null);
        this.playerName = playerName;
        this.playerIcon = new PlayerHeadIcon(StringUtils.fromString(playerId));
        this.wrapper = new GuiWrapper(this, container).disableSlotDrawing();
        this.inventoryName = playerName + "'s " + InvSeeInventories.MAIN.getInventory().getInventoryName();
    }

    @Override
    public GuiScreen getWrapper() {
        return wrapper;
    }

    @Override
    public int getY() {
        return super.getY() + 20;
    }

    @Override
    public void alignWidgets() {
        int textY = -13;
        textField.setText(inventoryName);
        if (textField.text.length > 1) {
            textY -= 3 * textField.text.length;
        }
        textField.setPos(28, textY);

        int highestSlot = container.getHighestSlot();
        int lowestSlot = container.getLowestSlot();
        topY = wrapper.guiTop + highestSlot;
        setHeight(lowestSlot - highestSlot);
        int xOffset = 0;
        int yOffset = 0;
        for (Widget widget : widgets) {
            if (!(widget instanceof Button)) continue;
            widget.setX(-18 - xOffset * 16);
            widget.setY(lowestSlot - 72 + yOffset * 16);
            if (yOffset % 5 == 4) {
                xOffset++;
                yOffset = 0;
            } else {
                yOffset++;
            }
        }
    }

    @Override
    public void drawForeground(Theme theme, int x, int y, int w, int h) {
        playerIcon.draw(x + 8, topY - 18, 16, 16);
    }

    @Override
    public void drawBackground(Theme theme, int x, int y, int w, int h) {
        super.drawBackground(theme, x, topY - 20, w, height + 42);
        GuiHelper.setupDrawing();

        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.inventorySlots.get(i);
            theme.drawContainerSlot(x + slot.xDisplayPosition, y + slot.yDisplayPosition, 16, 16);
            if (i >= container.getNonPlayerSlots() || slot.getHasStack()) continue;
            Icon overlay = container.getActiveInventory().getInventory().getSlotOverlay(slot);
            if (overlay != null) {
                overlay.draw(x + slot.xDisplayPosition, y + slot.yDisplayPosition, 16, 16);
            }
        }
    }

    @Override
    public void addWidgets() {
        add(textField = new TextField(this) {

            @Override
            public int getY() {
                return topY + posY;
            }
        }.setColor(Color4I.DARK_GRAY).setScale(0.9f).setMaxWidth(165).setSpacing(8));
        for (InvSeeInventories inventory : inventories.keySet()) {
            IModdedInventory moddedInv = inventory.getInventory();
            add(
                    new SimpleButton(
                            this,
                            moddedInv.getButtonText(),
                            moddedInv.getButtonIcon(),
                            (a, b) -> switchInventory(inventory)) {

                        @Override
                        public void drawBackground(Theme theme, int x, int y, int w, int h) {
                            BUTTON_BACKGROUND.draw(x, y, w, h);
                        }

                        @Override
                        public int getY() {
                            return wrapper.guiTop + posY;
                        }
                    });
        }
    }

    public void switchInventory(InvSeeInventories inventory) {
        if (container.getActiveInventory() == inventory) return;
        container.setActiveInventory(inventory);
        inventoryName = playerName + "'s " + inventory.getInventory().getInventoryName();
        alignWidgets();
        new MessageInvseeSwitch(inventory).sendToServer();
    }

    private static class GuiWrapper extends GuiContainerWrapper {

        private final GuiInvseeContainer gui;
        private int tempMouseY;
        private int oldTop;

        private GuiWrapper(GuiInvseeContainer gui, Container container) {
            super(gui, container);
            this.gui = gui;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            if (oldTop != guiTop) {
                gui.alignWidgets();
                oldTop = guiTop;
            }
            super.drawScreen(mouseX, mouseY, partialTicks);
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int button) {
            // this is a little hack to allow the slots to be above guiTop without having to shift the entire gui
            if (mouseY < guiTop && mouseY > gui.topY) {
                tempMouseY = mouseY;
                super.mouseClicked(mouseX, guiTop, button);
                tempMouseY = 0;
                return;
            }

            super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected boolean func_146978_c(int left, int top, int right, int bottom, int pointX, int pointY) {
            if (tempMouseY != 0) {
                pointY = tempMouseY;
            }
            return super.func_146978_c(left, top, right, bottom, pointX, pointY);
        }
    }
}
