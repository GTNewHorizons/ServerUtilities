package serverutils.lib.icon;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.io.DataReader;
import serverutils.lib.util.StringUtils;

public class PlayerHeadIcon extends ImageIcon {

    private static class ThreadLoadSkin extends SimpleTexture {

        private final PlayerHeadIcon icon;
        private final IImageBuffer imageBuffer;
        private BufferedImage bufferedImage;
        private Thread imageThread;
        private boolean textureUploaded;

        public ThreadLoadSkin(PlayerHeadIcon i) {
            super(SkinManager.field_152793_a);
            icon = i;
            imageBuffer = new ImageBufferDownload();
        }

        @Override
        public int getGlTextureId() {
            if (!textureUploaded) {
                if (bufferedImage != null) {
                    deleteGlTexture();
                    TextureUtil.uploadTextureImage(super.getGlTextureId(), bufferedImage);
                    textureUploaded = true;
                }
            }

            return super.getGlTextureId();
        }

        @Override
        public void loadTexture(IResourceManager resourceManager) throws IOException {
            if (bufferedImage == null) {
                super.loadTexture(resourceManager);
            }

            if (imageThread == null) {
                imageThread = new Thread("Skin Downloader for " + icon.uuid) {

                    @Override
                    public void run() {
                        String imageUrl = "";

                        try {
                            JsonObject json = DataReader.get(
                                    new URL(
                                            "https://sessionserver.mojang.com/session/minecraft/profile/"
                                                    + StringUtils.fromUUID(icon.uuid)),
                                    DataReader.JSON,
                                    Minecraft.getMinecraft().getProxy()).json().getAsJsonObject();

                            for (JsonElement element : json.get("properties").getAsJsonArray()) {
                                if (element.isJsonObject()
                                        && element.getAsJsonObject().get("name").getAsString().equals("textures")) {
                                    String base64 = new String(
                                            Base64.getDecoder()
                                                    .decode(element.getAsJsonObject().get("value").getAsString()),
                                            StandardCharsets.UTF_8);
                                    JsonObject json1 = DataReader.get(base64).json().getAsJsonObject().get("textures")
                                            .getAsJsonObject();

                                    if (json1.has("SKIN")) {
                                        imageUrl = json1.get("SKIN").getAsJsonObject().get("url").getAsString();
                                    }
                                }
                            }
                        } catch (Exception ignored) {}

                        if (imageUrl.isEmpty()) {
                            return;
                        }

                        try {
                            bufferedImage = imageBuffer.parseUserSkin(
                                    DataReader
                                            .get(new URL(imageUrl), DataReader.PNG, Minecraft.getMinecraft().getProxy())
                                            .image());
                        } catch (Exception ignored) {}
                    }
                };

                imageThread.setDaemon(true);
                imageThread.start();
            }
        }
    }

    public final UUID uuid;

    public PlayerHeadIcon(@Nullable UUID id) {
        super(id == null ? SkinManager.field_152793_a : new ResourceLocation("skins/" + StringUtils.fromUUID(id)));
        uuid = id;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void bindTexture() {
        TextureManager manager = Minecraft.getMinecraft().getTextureManager();
        ITextureObject img = manager.getTexture(texture);

        if (img == null) {
            img = new ThreadLoadSkin(this);
            manager.loadTexture(texture, img);
        }

        GlStateManager.bindTexture(img.getGlTextureId());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void draw(int x, int y, int w, int h) {
        bindTexture();
        GuiHelper.drawTexturedRect(x, y, w, h, color, 0.125D, 0.25D, 0.25D, 0.50D);
        GuiHelper.drawTexturedRect(x, y, w, h, color, 0.625D, 0.25D, 0.75D, 0.50D);
    }

    @Override
    public String toString() {
        return "player:" + uuid;
    }
}
