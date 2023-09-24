package serverutils.serverlib.lib.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import serverutils.serverlib.client.GlStateManager;
import serverutils.serverlib.lib.util.misc.MouseButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiContainerWrapper extends GuiContainer implements IGuiWrapper
{
	private GuiBase wrappedGui;
	private boolean drawSlots = true;
	private List<String> tempTextList = new ArrayList<>();

	public GuiContainerWrapper(GuiBase g, Container c)
	{
		super(c);
		wrappedGui = g;
	}

	public GuiContainerWrapper disableSlotDrawing()
	{
		drawSlots = false;
		return this;
	}

	@Override
	public void initGui()
	{
		super.initGui();
		wrappedGui.initGui();
		guiLeft = wrappedGui.getX();
		guiTop = wrappedGui.getY();
		xSize = wrappedGui.width;
		ySize = wrappedGui.height;
	}

	@Override
	public boolean doesGuiPauseGame()
	{
		return wrappedGui.doesGuiPauseGame();
	}

	@Override
	protected final void mouseClicked(int mouseX, int mouseY, int button) throws IOException
	{
		wrappedGui.updateMouseOver(mouseX, mouseY);

		if (button == MouseButton.BACK.id)
		{
			wrappedGui.onBack();
		}
		else if (!wrappedGui.mousePressed(MouseButton.get(button)))
		{
			super.mouseClicked(mouseX, mouseY, button);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button)
	{
		wrappedGui.updateMouseOver(mouseX, mouseY);
		wrappedGui.mouseReleased(MouseButton.get(button));
		super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	protected void keyTyped(char keyChar, int key) throws IOException
	{
		if (wrappedGui.keyPressed(key, keyChar))
		{
			return;
		}
		else if (key == Keyboard.KEY_BACK)
		{
			wrappedGui.onBack();
			return;
		}
		else if (wrappedGui.onClosedByKey(key))
		{
			wrappedGui.closeGui(false);
			return;
		}

		super.keyTyped(keyChar, key);
	}

	@Override
	public void handleMouseInput()
	{
		super.handleMouseInput();
		int scroll = Mouse.getEventDWheel();

		if (scroll != 0)
		{
			wrappedGui.mouseScrolled(scroll / 120);
		}
	}

	@Override
	public void handleKeyboardInput()
	{
		if (!(Keyboard.getEventKey() == 0 && Keyboard.getEventCharacter() >= ' ' || Keyboard.getEventKeyState()))
		{
			wrappedGui.keyReleased(Keyboard.getEventKey());
		}
		else
		{
			super.handleKeyboardInput();
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mx, int my)
	{
		if (wrappedGui.fixUnicode)
		{
			GuiHelper.setFixUnicode(true);
		}

		Theme theme = wrappedGui.getTheme();
		GuiHelper.setupDrawing();
		drawDefaultBackground();
		GuiHelper.setupDrawing();
		wrappedGui.draw(theme, guiLeft, guiTop, xSize, ySize);

		if (drawSlots)
		{
			GuiHelper.setupDrawing();

			for (Object s : inventorySlots.inventorySlots)
			{
				Slot slot = (Slot) s;
				theme.drawContainerSlot(guiLeft + slot.xDisplayPosition, guiTop + slot.yDisplayPosition, 16, 16);
			}
		}

		if (wrappedGui.fixUnicode)
		{
			GuiHelper.setFixUnicode(false);
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		if (wrappedGui.fixUnicode)
		{
			GuiHelper.setFixUnicode(true);
		}

		GlStateManager.pushMatrix();
		GlStateManager.translate(-guiLeft, -guiTop, 0D);
		GuiHelper.setupDrawing();

		Theme theme = wrappedGui.getTheme();
		wrappedGui.drawForeground(theme, guiLeft, guiTop, xSize, ySize);

		if (wrappedGui.contextMenu != null)
		{
			wrappedGui.contextMenu.addMouseOverText(tempTextList);
		}
		else
		{
			wrappedGui.addMouseOverText(tempTextList);
		}

		if (tempTextList.isEmpty())
		{
			Object object = wrappedGui.getIngredientUnderMouse();

			if (object instanceof WrappedIngredient && ((WrappedIngredient) object).tooltip)
			{
				Object ingredient = WrappedIngredient.unwrap(object);

				if (ingredient instanceof ItemStack && !((ItemStack) ingredient).isEmpty())
				{
					renderToolTip((ItemStack) ingredient, mouseX, mouseY);
				}
			}
		}
		else
		{
			GuiUtils.drawHoveringText(tempTextList, mouseX, Math.max(mouseY, 18), wrappedGui.getScreen().getScaledWidth(), wrappedGui.getScreen().getScaledHeight(), 0, theme.getFont());
		}

		tempTextList.clear();

		if (wrappedGui.contextMenu == null)
		{
			renderHoveredToolTip(mouseX, mouseY);
		}

		GlStateManager.popMatrix();

		if (wrappedGui.fixUnicode)
		{
			GuiHelper.setFixUnicode(false);
		}
	}

	@Override
	public void drawDefaultBackground()
	{
		if (wrappedGui.drawDefaultBackground())
		{
			super.drawDefaultBackground();
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		wrappedGui.updateGui(mouseX, mouseY, partialTicks);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();
		wrappedGui.tick();
	}

	@Override
	public GuiBase getGui()
	{
		return wrappedGui;
	}
}