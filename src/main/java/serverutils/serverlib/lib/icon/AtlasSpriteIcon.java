package serverutils.serverlib.lib.icon;

import serverutils.serverlib.lib.client.IPixelBuffer;
import serverutils.serverlib.lib.client.PixelBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

public class AtlasSpriteIcon extends Icon
{
	public final String name;
	public Color4I color;

	AtlasSpriteIcon(String n)
	{
		name = n;
		color = Color4I.WHITE;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void draw(int x, int y, int w, int h)
	{
		TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
		textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(name);
		int r = color.redi();
		int g = color.greeni();
		int b = color.bluei();
		int a = color.alphai();
		buffer.pos(x, y + h, 0D).tex(sprite.getMinU(), sprite.getMaxV()).color(r, g, b, a).endVertex();
		buffer.pos(x + w, y + h, 0D).tex(sprite.getMaxU(), sprite.getMaxV()).color(r, g, b, a).endVertex();
		buffer.pos(x + w, y, 0D).tex(sprite.getMaxU(), sprite.getMinV()).color(r, g, b, a).endVertex();
		buffer.pos(x, y, 0D).tex(sprite.getMinU(), sprite.getMinV()).color(r, g, b, a).endVertex();
		tessellator.draw();
		textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
	}

	@Override
	public String toString()
	{
		return name;
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
			ResourceLocation rl = new ResourceLocation(name);
			return PixelBuffer.from(Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(rl.getNamespace(), "textures/" + rl.getPath() + ".png")).getInputStream());
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	@Override
	public AtlasSpriteIcon copy()
	{
		return new AtlasSpriteIcon(name);
	}

	@Override
	public AtlasSpriteIcon withColor(Color4I color)
	{
		AtlasSpriteIcon icon = copy();
		icon.color = color;
		return icon;
	}

	@Override
	public AtlasSpriteIcon withTint(Color4I c)
	{
		return withColor(color.withTint(c));
	}
}