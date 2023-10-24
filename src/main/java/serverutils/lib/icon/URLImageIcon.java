package serverutils.lib.icon;

import java.io.File;
import java.net.URI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.client.IPixelBuffer;
import serverutils.lib.client.PixelBuffer;
import serverutils.lib.io.DataReader;

public class URLImageIcon extends ImageIcon {

    public final URI uri;
    private final String url;

    public URLImageIcon(ResourceLocation tex, URI _uri) {
        super(tex);
        uri = _uri;
        url = uri.toString();
    }

    public URLImageIcon(URI uri) {
        this(new ResourceLocation(uri.toString()), uri);
    }

    @Override
    public URLImageIcon copy() {
        URLImageIcon icon = new URLImageIcon(texture, uri);
        icon.minU = minU;
        icon.minV = minV;
        icon.maxU = maxU;
        icon.maxV = maxV;
        icon.tileSize = tileSize;
        return icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void bindTexture() {
        TextureManager manager = Minecraft.getMinecraft().getTextureManager();
        ITextureObject img = manager.getTexture(texture);

        if (img == null) {
            if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {
                img = new ThreadDownloadImageData(null, url, MISSING_IMAGE, null);
            } else {
                File file = null;

                if (uri.getScheme().equals("file")) {
                    try {
                        file = new File(uri.getPath());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                if (file == null) {
                    file = new File(uri);
                }

                img = new ThreadDownloadImageData(file, url, MISSING_IMAGE, null);
            }

            manager.loadTexture(texture, img);
        }

        GlStateManager.bindTexture(img.getGlTextureId());
    }

    public String toString() {
        return url;
    }

    @Override
    public IPixelBuffer createPixelBuffer() {
        try {
            return PixelBuffer.from(DataReader.get(uri, Minecraft.getMinecraft().getProxy()).image());
        } catch (Exception ex) {
            return null;
        }
    }
}
