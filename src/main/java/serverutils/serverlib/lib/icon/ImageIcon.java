package serverutils.serverlib.lib.icon;

import com.google.common.base.Objects;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import serverutils.serverlib.ServerLib;
import serverutils.serverlib.lib.client.GlStateManager;
import serverutils.serverlib.lib.client.IPixelBuffer;
import serverutils.serverlib.lib.client.PixelBuffer;
import serverutils.serverlib.lib.gui.GuiHelper;

import javax.annotation.Nullable;

public class ImageIcon extends Icon
{
	public static final ResourceLocation MISSING_IMAGE = new ResourceLocation(ServerLib.MOD_ID, "textures/gui/missing_image.png");

	public final ResourceLocation texture;
	public double minU, minV, maxU, maxV;
	public double tileSize;
	public Color4I color;

	public ImageIcon(ResourceLocation tex)
	{
		texture = tex;
		minU = 0;
		minV = 0;
		maxU = 1;
		maxV = 1;
		tileSize = 0;
		color = Color4I.WHITE;
	}

	@Override
	public ImageIcon copy()
	{
		ImageIcon icon = new ImageIcon(texture);
		icon.minU = minU;
		icon.minV = minV;
		icon.maxU = maxU;
		icon.maxV = maxV;
		icon.tileSize = tileSize;
		return icon;
	}

	@Override
	protected void setProperties(IconProperties properties)
	{
		super.setProperties(properties);
		minU = properties.getDouble("u0", minU);
		minV = properties.getDouble("v0", minV);
		maxU = properties.getDouble("u1", maxU);
		maxV = properties.getDouble("v1", maxV);
		tileSize = properties.getDouble("tile_size", tileSize);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void bindTexture()
	{
		TextureManager manager = Minecraft.getMinecraft().getTextureManager();
		ITextureObject tex = manager.getTexture(texture);

		if (tex == null)
		{
			tex = new SimpleTexture(texture);
			manager.loadTexture(texture, tex);
		}

		GlStateManager.bindTexture(tex.getGlTextureId());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void draw(int x, int y, int w, int h)
	{
		bindTexture();

		if (tileSize <= 0D)
		{
			GuiHelper.drawTexturedRect(x, y, w, h, color, minU, minV, maxU, maxV);
		}
		else
		{
			int r = color.redi();
			int g = color.greeni();
			int b = color.bluei();
			int a = color.alphai();

			Tessellator tessellator = Tessellator.instance;
			BufferBuilder buffer = tessellator.getBuffer();
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
			buffer.pos(x, y + h, 0).tex(x / tileSize, (y + h) / tileSize).color(r, g, b, a).endVertex();
			buffer.pos(x + w, y + h, 0).tex((x + w) / tileSize, (y + h) / tileSize).color(r, g, b, a).endVertex();
			buffer.pos(x + w, y, 0).tex((x + w) / tileSize, y / tileSize).color(r, g, b, a).endVertex();
			buffer.pos(x, y, 0).tex(x / tileSize, y / tileSize).color(r, g, b, a).endVertex();
			tessellator.draw();
		}
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(texture, minU, minV, maxU, maxV);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
		{
			return true;
		}
		else if (o instanceof ImageIcon)
		{
			ImageIcon img = (ImageIcon) o;
			return texture.equals(img.texture) && minU == img.minU && minV == img.minV && maxU == img.maxU && maxV == img.maxV;
		}
		return false;
	}

	@Override
	public String toString()
	{
		return texture.toString();
	}

	@Override
	public ImageIcon withColor(Color4I color)
	{
		ImageIcon icon = copy();
		icon.color = color;
		return icon;
	}

	@Override
	public ImageIcon withTint(Color4I c)
	{
		return withColor(color.withTint(c));
	}

	@Override
	public ImageIcon withUV(double u0, double v0, double u1, double v1)
	{
		ImageIcon icon = copy();
		icon.minU = u0;
		icon.minV = v0;
		icon.maxU = u1;
		icon.maxV = v1;
		return icon;
	}

	@Override
	public boolean hasPixelBuffer()
	{
		return true;
	}

	@Override
	@Nullable
	public IPixelBuffer createPixelBuffer()
	{
		try
		{
			return PixelBuffer.from(Minecraft.getMinecraft().getResourceManager().getResource(texture).getInputStream());
		}
		catch (Exception ex)
		{
			return null;
		}
	}
}