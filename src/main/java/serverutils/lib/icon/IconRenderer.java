package serverutils.lib.icon;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import serverutils.handlers.ServerUtilitiesClientEventHandler;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.client.IPixelBuffer;

public class IconRenderer {

    private static class IconCallbackPair implements Runnable {

        private Icon icon;
        private ImageCallback callback;

        @Override
        public void run() {
            QUEUE.add(this);
            ServerUtilitiesClientEventHandler.shouldRenderIcons = true;
        }
    }

    private static final Collection<IconCallbackPair> QUEUE = new LinkedList<>();
    private static Image nullImage = null;
    private static Map<Icon, Image> imageCache = new HashMap<>();

    public static void clearCache() {
        imageCache = new HashMap<>();
    }

    public static Image getNullImage() {
        if (nullImage == null) {
            try {
                nullImage = ImageIO.read(
                        IconRenderer.class.getResourceAsStream("/assets/serverutilities/textures/icons/cancel.png"));
            } catch (IOException e) {
                // nop
            }
        }

        return nullImage;
    }

    public static boolean load(@Nullable Icon icon, ImageCallback callback) {
        if (icon == null) {
            callback.imageLoaded(false, null);
            return true;
        } else if (icon.isEmpty()) {
            callback.imageLoaded(false, getNullImage());
            return true;
        }

        Image image = imageCache.get(icon);

        if (image != null) {
            callback.imageLoaded(false, image);
            return true;
        }

        if (icon.hasPixelBuffer()) {
            IPixelBuffer buffer = icon.createPixelBuffer();

            if (buffer == null) {
                image = getNullImage();
            } else {
                int w = buffer.getWidth();
                int h = buffer.getHeight();
                BufferedImage image_ = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                image_.setRGB(0, 0, w, h, buffer.getPixels(), 0, w);
                image = image_;
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
        Minecraft.getMinecraft().func_152344_a(pair);

        return false;
    }

    /**
     * Modified version of BlockRenderer mod code
     */
    public static void render() {
        if (QUEUE.isEmpty()) {
            return;
        }

        IconCallbackPair[] queued = QUEUE.toArray(new IconCallbackPair[0]);
        QUEUE.clear();

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution res = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int size = Math.min(Math.min(mc.displayHeight, mc.displayWidth), 64);

        mc.entityRenderer.setupOverlayRendering();
        RenderHelper.enableGUIStandardItemLighting();
        float scale = size / (16F * res.getScaleFactor());
        GlStateManager.translate(0, 0, -(scale * 100F));

        GlStateManager.scale(scale, scale, scale);

        RenderItem renderItem = RenderItem.getInstance();
        float oldZLevel = renderItem.zLevel;
        renderItem.zLevel = -50;

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableDepth();
        GlStateManager.enableBlend();
        GlStateManager
                .tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();

        int[] pixels = new int[size * size];
        AffineTransform at = new AffineTransform();
        at.concatenate(AffineTransform.getScaleInstance(1, -1));
        at.concatenate(AffineTransform.getTranslateInstance(0, -size));
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        for (IconCallbackPair pair : queued) {
            GlStateManager.pushMatrix();
            GlStateManager.clearColor(0F, 0F, 0F, 0F);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            pair.icon.drawStatic(0, 0, 16, 16);
            GlStateManager.popMatrix();

            try {
                ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
                GL11.glReadBuffer(GL11.GL_BACK);
                GlStateManager.glGetError(); // FIXME: For some reason it throws error here, but it still works. Calling
                // this to not spam console
                GL11.glReadPixels(
                        0,
                        Minecraft.getMinecraft().displayHeight - size,
                        size,
                        size,
                        GL12.GL_BGRA,
                        GL11.GL_UNSIGNED_BYTE,
                        buf);
                buf.asIntBuffer().get(pixels);
                img.setRGB(0, 0, size, size, pixels, 0, size);
                BufferedImage flipped = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = flipped.createGraphics();
                g.transform(at);
                g.drawImage(img, 0, 0, null);
                g.dispose();
                pixels = flipped.getRGB(0, 0, size, size, pixels, 0, size);

                BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                image.setRGB(0, 0, size, size, pixels, 0, size);
                imageCache.put(pair.icon, image);
                pair.callback.imageLoaded(true, image);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        GlStateManager.disableLighting();
        GlStateManager.disableColorMaterial();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        renderItem.zLevel = oldZLevel;
    }
}
