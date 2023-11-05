package serverutils.lib.icon;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.client.IPixelBuffer;
import serverutils.lib.client.PixelBuffer;

public class AtlasSpriteIcon extends Icon {

    public final String name;
    public Color4I color;

    AtlasSpriteIcon(String n) {
        name = n;
        color = Color4I.WHITE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void draw(int x, int y, int w, int h) {
        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        textureManager.bindTexture(TextureMap.locationBlocksTexture);
        // textureManager.getTexture(TextureMap.locationItemsTexture).setBlurMipmap(false, false);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(name);
        int r = color.redi();
        int g = color.greeni();
        int b = color.bluei();
        int a = color.alphai();
        tessellator.setColorRGBA(r, g, b, a);
        tessellator.setTextureUV(sprite.getMinU(), sprite.getMaxV());
        tessellator.addVertex(x, y + h, 0);
        tessellator.setColorRGBA(r, g, b, a);
        tessellator.setTextureUV(sprite.getMaxU(), sprite.getMaxV());
        tessellator.addVertex(x + w, y + h, 0);
        tessellator.setColorRGBA(r, g, b, a);
        tessellator.setTextureUV(sprite.getMaxU(), sprite.getMinV());
        tessellator.addVertex(x + w, y, 0);
        tessellator.setColorRGBA(r, g, b, a);
        tessellator.setTextureUV(sprite.getMinU(), sprite.getMinV());
        tessellator.addVertex(x, y, 0);
        tessellator.draw();
        // textureManager.getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean hasPixelBuffer() {
        return true;
    }

    @Override
    @Nullable
    public IPixelBuffer createPixelBuffer() {
        try {
            ResourceLocation rl = new ResourceLocation(name);
            return PixelBuffer.from(
                    Minecraft.getMinecraft().getResourceManager().getResource(
                            new ResourceLocation(rl.getResourceDomain(), "textures/" + rl.getResourcePath() + ".png"))
                            .getInputStream());
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public AtlasSpriteIcon copy() {
        return new AtlasSpriteIcon(name);
    }

    @Override
    public AtlasSpriteIcon withColor(Color4I color) {
        AtlasSpriteIcon icon = copy();
        icon.color = color;
        return icon;
    }

    @Override
    public AtlasSpriteIcon withTint(Color4I c) {
        return withColor(color.withTint(c));
    }
}
