package serverutils.client.gui;

public enum LayerBadge // implements LayerRenderer<AbstractClientPlayer>
{
    // INSTANCE;
    //
    // @Override
    // public void doRenderLayer(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float
    // partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    // {
    // if (ServerUtilitiesClientConfig.general.render_badges && !player.isInvisible())
    // {
    // UUID id = player.getGameProfile().getId();
    // Icon tex = ServerUtilitiesClientEventHandler.getBadge(id);
    //
    // if (tex.isEmpty())
    // {
    // return;
    // }
    //
    // GlStateManager.disableLighting();
    // GlStateManager.disableCull();
    // GlStateManager.enableTexture2D();
    // GlStateManager.enableBlend();
    // GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    //
    // ClientUtils.pushMaxBrightness();
    // GlStateManager.pushMatrix();
    //
    // GlStateManager.translate(0.04F, 0.01F, 0.86F);
    //
    // if (player.isSneaking())
    // {
    // GlStateManager.rotate(25F, 1F, 0F, 0F);
    // GlStateManager.translate(0F, -0.18F, 0F);
    // }
    //
    // ItemStack armor = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
    // if (ServerUtilitiesItems.SILENTGEMS_CHESTPLATE != Items.AIR && armor.getItem() ==
    // ServerUtilities.SILENTGEMS_CHESTPLATE)
    // {
    // GlStateManager.translate(0F, 0F, -0.1F);
    // }
    // else if (!armor.isEmpty() && armor.getItem().isValidArmor(armor, EntityEquipmentSlot.CHEST, player))
    // {
    // GlStateManager.translate(0F, 0F, -0.0625F);
    // }
    // else if (player.isWearing(EnumPlayerModelParts.JACKET))
    // {
    // GlStateManager.translate(0F, 0F, -0.02125F);
    // }
    //
    // GlStateManager.translate(0F, 0F, -1F);
    // GlStateManager.scale(0.2D, 0.2D, 0.125D);
    // GlStateManager.color(1F, 1F, 1F, 1F);
    // GlStateManager.disableCull();
    // tex.draw(0, 0, 1, 1);
    // GlStateManager.enableCull();
    // ClientUtils.popBrightness();
    // GlStateManager.popMatrix();
    // }
    // }
    //
    // @Override
    // public boolean shouldCombineTextures()
    // {
    // return false;
    // }
}
