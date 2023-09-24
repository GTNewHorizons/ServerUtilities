package serverutils.serverlib.lib.gui.misc;


import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import serverutils.serverlib.lib.client.GlStateManager;
import serverutils.serverlib.lib.client.CachedVertexData;
import serverutils.serverlib.lib.gui.Button;
import serverutils.serverlib.lib.gui.GuiBase;
import serverutils.serverlib.lib.gui.GuiHelper;
import serverutils.serverlib.lib.gui.Panel;
import serverutils.serverlib.lib.gui.Theme;
import serverutils.serverlib.lib.gui.Widget;
import serverutils.serverlib.lib.gui.WidgetLayout;
import serverutils.serverlib.lib.icon.Color4I;
import serverutils.serverlib.lib.math.ChunkDimPos;
import serverutils.serverlib.lib.math.MathUtils;
import serverutils.serverlib.lib.util.misc.MouseButton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GuiChunkSelectorBase extends GuiBase
{
	protected enum Corner
	{
		BOTTOM_LEFT,
		BOTTOM_RIGHT,
		TOP_LEFT
	}

	public static final int TILE_SIZE = 12;
	private static final CachedVertexData GRID = new CachedVertexData(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

	static
	{
		GRID.color.set(128, 128, 128, 50);

		for (int x = 0; x <= ChunkSelectorMap.TILES_GUI; x++)
		{
			GRID.pos(x * TILE_SIZE, 0D);
			GRID.pos(x * TILE_SIZE, ChunkSelectorMap.TILES_GUI * TILE_SIZE, 0D);
		}

		for (int y = 0; y <= ChunkSelectorMap.TILES_GUI; y++)
		{
			GRID.pos(0D, y * TILE_SIZE, 0D);
			GRID.pos(ChunkSelectorMap.TILES_GUI * TILE_SIZE, y * TILE_SIZE, 0D);
		}
	}

	public static class MapButton extends Button
	{
		public final GuiChunkSelectorBase gui;
		public final ChunkDimPos chunkPos;
		public final int index;
		private boolean isSelected = false;

		private MapButton(GuiChunkSelectorBase g, int i)
		{
			super(g);
			gui = g;
			index = i;
			setPosAndSize((index % ChunkSelectorMap.TILES_GUI) * TILE_SIZE, (index / ChunkSelectorMap.TILES_GUI) * TILE_SIZE, TILE_SIZE, TILE_SIZE);
			chunkPos = new ChunkDimPos(gui.startX + (i % ChunkSelectorMap.TILES_GUI), gui.startZ + (i / ChunkSelectorMap.TILES_GUI));
		}

		@Override
		public void onClicked(MouseButton button)
		{
			GuiHelper.playClickSound();
			gui.currentSelectionMode = gui.getSelectionMode(button);

			if (gui.currentSelectionMode == -1)
			{
				gui.onChunksSelected(Collections.singleton(chunkPos));
			}
		}

		@Override
		public void addMouseOverText(List<String> list)
		{
			gui.addButtonText(this, list);
		}

		@Override
		public void draw(Theme theme, int x, int y, int w, int h)
		{
			if (!isSelected && gui.currentSelectionMode != -1 && gui.isMouseOver(this))
			{
				isSelected = true;
			}

			if (isSelected || gui.isMouseOver(this))
			{
				Color4I.WHITE.withAlpha(33).draw(x, y, TILE_SIZE, TILE_SIZE);
			}
		}
	}

	public int startX, startZ;
	private final MapButton[] mapButtons;
	private final Panel panelButtons;
	public int currentSelectionMode = -1;

	public GuiChunkSelectorBase()
	{
		startX = MathUtils.chunk(Minecraft.getMinecraft().thePlayer.posX) - ChunkSelectorMap.TILES_GUI2;
		startZ = MathUtils.chunk(Minecraft.getMinecraft().thePlayer.posZ) - ChunkSelectorMap.TILES_GUI2;

		panelButtons = new Panel(this)
		{
			@Override
			public void addWidgets()
			{
				addCornerButtons(panelButtons);
			}

			@Override
			public void alignWidgets()
			{
				int h = align(WidgetLayout.VERTICAL);
				int w = 0;

				for (Widget widget : widgets)
				{
					w = Math.max(w, widget.width);
				}

				panelButtons.setPosAndSize(getGui().width + 2, -2, w, h);
			}
		};

		mapButtons = new MapButton[ChunkSelectorMap.TILES_GUI * ChunkSelectorMap.TILES_GUI];

		for (int i = 0; i < mapButtons.length; i++)
		{
			mapButtons[i] = new MapButton(this, i);
		}
	}

	@Override
	public boolean onInit()
	{
		ChunkSelectorMap.getMap().resetMap(startX, startZ);
		return true;
	}

	@Override
	public void addWidgets()
	{
		for (MapButton b : mapButtons)
		{
			add(b);
		}

		add(panelButtons);
	}

	@Override
	public void alignWidgets()
	{
		setSize(ChunkSelectorMap.TILES_GUI * TILE_SIZE, ChunkSelectorMap.TILES_GUI * TILE_SIZE);
		panelButtons.alignWidgets();
	}

	@Override
	public void drawBackground(Theme theme, int x, int y, int w, int h)
	{
		int currentStartX = MathUtils.chunk(Minecraft.getMinecraft().thePlayer.posX) - ChunkSelectorMap.TILES_GUI2;
		int currentStartZ = MathUtils.chunk(Minecraft.getMinecraft().thePlayer.posZ) - ChunkSelectorMap.TILES_GUI2;

		if (currentStartX != startX || currentStartZ != startZ)
		{
			startX = currentStartX;
			startZ = currentStartZ;

			for (int i = 0; i < mapButtons.length; i++)
			{
				mapButtons[i] = new MapButton(this, i);
			}

			ChunkSelectorMap.getMap().resetMap(startX, startZ);
		}

		GlStateManager.color(1F, 1F, 1F, 1F);
		Color4I.BLACK.draw(x - 2, y - 2, w + 4, h + 4);

		ChunkSelectorMap.getMap().drawMap(this, x, y, startX, startZ);

		GlStateManager.color(1F, 1F, 1F, 1F);

		for (MapButton mapButton : mapButtons)
		{
			mapButton.draw(theme, mapButton.getX(), mapButton.getY(), mapButton.width, mapButton.height);
		}

		GlStateManager.disableTexture2D();
		GlStateManager.glLineWidth(1F);

		Tessellator tessellator = Tessellator.instance;
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.setTranslation(mapButtons[0].getX(), mapButtons[0].getY(), 0D);
		//GlStateManager.color(1F, 1F, 1F, GuiScreen.isCtrlKeyDown() ? 0.2F : 0.7F);
		GlStateManager.color(1F, 1F, 1F, 1F);

		if (!isKeyDown(Keyboard.KEY_TAB))
		{
			drawArea(tessellator, buffer);
		}

		GRID.draw(tessellator, buffer);
		buffer.setTranslation(0D, 0D, 0D);
		GlStateManager.enableTexture2D();
		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	@Override
	public void mouseReleased(MouseButton button)
	{
		super.mouseReleased(button);

		if (currentSelectionMode != -1)
		{
			Collection<ChunkDimPos> c = new ArrayList<>();

			for (MapButton b : mapButtons)
			{
				if (b.isSelected)
				{
					c.add(b.chunkPos);
					b.isSelected = false;
				}
			}

			onChunksSelected(c);
			currentSelectionMode = -1;
		}
	}

	@Override
	public void drawForeground(Theme theme, int x, int y, int w, int h)
	{
		int lineSpacing = theme.getFontHeight() + 1;
		List<String> tempTextList = new ArrayList<>();
		addCornerText(tempTextList, Corner.BOTTOM_RIGHT);

		for (int i = 0; i < tempTextList.size(); i++)
		{
			String s = tempTextList.get(i);
			theme.drawString(s, getScreen().getScaledWidth() - theme.getStringWidth(s) - 2, getScreen().getScaledHeight() - (tempTextList.size() - i) * lineSpacing, Theme.SHADOW);
		}

		tempTextList.clear();

		addCornerText(tempTextList, Corner.BOTTOM_LEFT);

		for (int i = 0; i < tempTextList.size(); i++)
		{
			theme.drawString(tempTextList.get(i), 2, getScreen().getScaledHeight() - (tempTextList.size() - i) * lineSpacing, Theme.SHADOW);
		}

		tempTextList.clear();

		addCornerText(tempTextList, Corner.TOP_LEFT);

		for (int i = 0; i < tempTextList.size(); i++)
		{
			theme.drawString(tempTextList.get(i), 2, 2 + i * lineSpacing, Theme.SHADOW);
		}

		super.drawForeground(theme, x, y, w, h);
	}

	public int getSelectionMode(MouseButton button)
	{
		return -1;
	}

	public void onChunksSelected(Collection<ChunkDimPos> chunks)
	{
	}

	public void drawArea(Tessellator tessellator, BufferBuilder buffer)
	{
	}

	public void addCornerButtons(Panel panel)
	{
	}

	public void addCornerText(List<String> list, Corner corner)
	{
	}

	public void addButtonText(MapButton button, List<String> list)
	{
	}
}
