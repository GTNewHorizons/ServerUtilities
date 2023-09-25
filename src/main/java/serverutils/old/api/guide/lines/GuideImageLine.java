package serverutils.old.api.guide.lines;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import com.google.gson.*;

import cpw.mods.fml.relauncher.*;
import latmod.lib.LMUtils;
import serverutils.lib.*;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.old.api.guide.GuidePage;
import serverutils.old.mod.client.gui.guide.*;

/**
 * Created by LatvianModder on 23.03.2016.
 */
public class GuideImageLine extends GuideExtendedTextLine {

    private String imageURL;
    private TextureCoords texture;
    private double displayW, displayH, displayS;

    public GuideImageLine(GuidePage c) {
        super(c, null);
    }

    @SideOnly(Side.CLIENT)
    public TextureCoords getImage() {
        if (texture == TextureCoords.nullTexture) return null;
        else if (texture != null) return texture;
        else if (imageURL == null) return null;

        texture = TextureCoords.nullTexture;

        try {
            File file = new File(ServerUtilitiesLib.folderModpack, "images/" + imageURL);
            if (ServerUtilitiesLib.DEV_ENV)
                ServerUtilitiesLib.dev_logger.info("Loading Guide image: " + file.getAbsolutePath());
            BufferedImage img = ImageIO.read(file);
            ResourceLocation tex = ServerUtilitiesLibraryClient.mc.getTextureManager()
                    .getDynamicTextureLocation("serverutils_guide/" + imageURL, new DynamicTexture(img));
            texture = new TextureCoords(tex, 0D, 0D, img.getWidth(), img.getHeight(), img.getWidth(), img.getHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return texture;
    }

    @SideOnly(Side.CLIENT)
    public TextureCoords getDisplayImage() {
        TextureCoords img = getImage();
        if (img == null) return null;
        double w = (displayW > 0D) ? displayW
                : (displayS == 0D ? texture.width
                        : (displayS > 0D ? texture.width * displayS : ((double) texture.width / -displayS)));
        double h = (displayH > 0D) ? displayH
                : (displayS == 0D ? texture.height
                        : (displayS > 0D ? texture.height * displayS : ((double) texture.height / -displayS)));
        return new TextureCoords(texture.texture, 0D, 0D, w, h, w, h);
    }

    public void setImage(String img) {
        String imageURL0 = imageURL == null ? null : (imageURL + "");
        imageURL = img;
        if (!LMUtils.areObjectsEqual(imageURL0, imageURL, true)) texture = null;
        if (imageURL != null) text = null;
    }

    public void setImage(TextureCoords t) {
        texture = t;
        imageURL = null;
        text = null;
    }

    @SideOnly(Side.CLIENT)
    public ButtonGuideTextLine createWidget(GuiGuide gui) {
        if (getImage() == null) return null;
        return new ButtonGuideImage(gui, this);
    }

    public void func_152753_a(JsonElement e) {
        super.func_152753_a(e);

        displayW = displayH = displayS = 0D;

        JsonObject o = e.getAsJsonObject();

        setImage(o.has("image") ? o.get("image").getAsString() : null);

        if (o.has("scale")) {
            displayS = o.get("scale").getAsDouble();
        } else {
            if (o.has("width")) {
                displayW = o.get("width").getAsDouble();
            }

            if (o.has("height")) {
                displayH = o.get("height").getAsDouble();
            }
        }
    }

    public JsonElement getSerializableElement() {
        JsonObject o = (JsonObject) super.getSerializableElement();

        if (imageURL != null && !imageURL.isEmpty()) o.add("image", new JsonPrimitive(imageURL));

        return o;
    }
}
