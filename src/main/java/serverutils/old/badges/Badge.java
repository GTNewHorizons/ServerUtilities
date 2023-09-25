package serverutils.old.badges;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.*;
import latmod.lib.util.FinalIDObject;
import serverutils.lib.api.client.*;
import serverutils.lib.api.gui.GuiLM;

public class Badge extends FinalIDObject {

    public static final ResourceLocation defTex = new ResourceLocation("serverutils", "textures/failed_badge.png");
    public static final Badge emptyBadge = new Badge("-", null);

    // -- //

    public final String imageURL;
    private ResourceLocation textureURL = null;

    public Badge(String id, String url) {
        super(id);
        imageURL = url;
    }

    public String toString() {
        return getID() + " : " + imageURL;
    }

    @SideOnly(Side.CLIENT)
    public ResourceLocation getTexture() {
        if (imageURL == null) return null;

        if (textureURL == null) {
            textureURL = new ResourceLocation("serverutils", "textures/badges/" + getID());
            ServerUtilitiesLibraryClient.getDownloadImage(textureURL, imageURL, defTex, null);
        }

        return textureURL;
    }

    @SideOnly(Side.CLIENT)
    public void onPlayerRender(EntityPlayer ep) {
        ResourceLocation texture = getTexture();
        if (texture == null) return;

        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ServerUtilitiesLibraryClient.setTexture(texture);
        ServerUtilitiesLibraryClient.pushMaxBrightness();
        GlStateManager.pushMatrix();

        if (ep.isSneaking()) GlStateManager.rotate(25F, 1F, 0F, 0F);

        GlStateManager.translate(0.04F, 0.01F, 0.86F);

        ItemStack armor = ep.getEquipmentInSlot(3);
        if (armor != null && armor.getItem().isValidArmor(armor, 1, ep)) GlStateManager.translate(0F, 0F, -0.0625F);

        GlStateManager.translate(0F, 0F, -1F);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GuiLM.drawTexturedRectD(0D, 0D, 0D, 0.2D, 0.2D, 0D, 0D, 1D, 1D);

        ServerUtilitiesLibraryClient.popMaxBrightness();
        GlStateManager.popMatrix();
    }
}
