package serverutils.serverlib.lib.icon;

import serverutils.serverlib.client.GlStateManager;
import serverutils.serverlib.client.ServerLibClientEventHandler;
import serverutils.serverlib.lib.client.IPixelBuffer;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import cpw.mods.fml.common.Loader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class IconRenderer
{
	private static class IconCallbackPair implements Runnable
	{
		private Icon icon;
		private ImageCallback callback;

		@Override
		public void run()
		{
			QUEUE.add(this);
			ServerLibClientEventHandler.shouldRenderIcons = true;
		}
	}

	private static final Collection<IconCallbackPair> QUEUE = new LinkedList<>();
	private static Image nullImage = null;
	private static Map<Icon, Image> imageCache = new HashMap<>();

	public static void clearCache()
	{
		imageCache = new HashMap<>();
	}

	public static Image getNullImage()
	{
		if (nullImage == null)
		{
			if (Loader.isModLoaded("itemfilters"))
			{
				nullImage = new Image(IconRenderer.class.getResourceAsStream("/assets/itemfilters/textures/items/missing.png"));
			}
			else
			{
				nullImage = new Image(IconRenderer.class.getResourceAsStream("/assets/ftblib/textures/icons/cancel.png"));
			}
		}

		return nullImage;
	}

	public static boolean load(@Nullable Icon icon, ImageCallback callback)
	{
		if (icon == null)
		{
			callback.imageLoaded(false, null);
			return true;
		}
		else if (icon.isEmpty())
		{
			callback.imageLoaded(false, getNullImage());
			return true;
		}

		Image image = imageCache.get(icon);

		if (image != null)
		{
			callback.imageLoaded(false, image);
			return true;
		}

		if (icon.hasPixelBuffer())
		{
			IPixelBuffer buffer = icon.createPixelBuffer();

			if (buffer == null)
			{
				image = getNullImage();
			}
			else
			{
				int w = buffer.getWidth();
				int h = buffer.getHeight();
				image = new WritableImage(w, h);
				((WritableImage) image).getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), buffer.getPixels(), 0, w);
			}

			imageCache.put(icon, image);
			callback.imageLoaded(false, image);
			return true;
		}

		imageCache.put(icon, getNullImage());
		callback.imageLoaded(false, getNullImage());

		IconCallbackPair pair = new IconCallbackPair();
		pair.icon = icon;
		pair.callback = callback;
		Minecraft.getMinecraft().addScheduledTask(pair);

		return false;
	}

	/**
	 * Modified version of BlockRenderer mod code
	 */
	public static void render()
	{
		if (QUEUE.isEmpty())
		{
			return;
		}

		IconCallbackPair[] queued = QUEUE.toArray(new IconCallbackPair[0]);
		QUEUE.clear();

		Minecraft mc = Minecraft.getMinecraft();
		ScaledResolution res = new ScaledResolution(mc);
		int size = Math.min(Math.min(mc.displayHeight, mc.displayWidth), 64);

		mc.entityRenderer.setupOverlayRendering();
		RenderHelper.enableGUIStandardItemLighting();
		float scale = size / (16F * res.getScaleFactor());
		GlStateManager.translate(0, 0, -(scale * 100F));

		GlStateManager.scale(scale, scale, scale);

		float oldZLevel = mc.getRenderItem().zLevel;
		mc.getRenderItem().zLevel = -50;

		GlStateManager.enableRescaleNormal();
		GlStateManager.enableColorMaterial();
		GlStateManager.enableDepth();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.disableAlpha();

		int[] pixels = new int[size * size];
		AffineTransform at = new AffineTransform();
		at.concatenate(AffineTransform.getScaleInstance(1, -1));
		at.concatenate(AffineTransform.getTranslateInstance(0, -size));
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

		for (IconCallbackPair pair : queued)
		{
			GlStateManager.pushMatrix();
			GlStateManager.clearColor(0F, 0F, 0F, 0F);
			GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			pair.icon.drawStatic(0, 0, 16, 16);
			GlStateManager.popMatrix();

			try
			{
				ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
				GL11.glReadBuffer(GL11.GL_BACK);
				GlStateManager.glGetError(); //FIXME: For some reason it throws error here, but it still works. Calling this to not spam console
				GL11.glReadPixels(0, Minecraft.getMinecraft().displayHeight - size, size, size, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buf);
				buf.asIntBuffer().get(pixels);
				img.setRGB(0, 0, size, size, pixels, 0, size);
				BufferedImage flipped = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = flipped.createGraphics();
				g.transform(at);
				g.drawImage(img, 0, 0, null);
				g.dispose();
				pixels = flipped.getRGB(0, 0, size, size, pixels, 0, size);
				WritableImage image = new WritableImage(size, size);
				image.getPixelWriter().setPixels(0, 0, size, size, PixelFormat.getIntArgbInstance(), pixels, 0, size);
				imageCache.put(pair.icon, image);
				pair.callback.imageLoaded(true, image);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		GlStateManager.disableLighting();
		GlStateManager.disableColorMaterial();
		GlStateManager.disableDepth();
		GlStateManager.disableBlend();
		Minecraft.getMinecraft().getRenderItem().zLevel = oldZLevel;
	}
}