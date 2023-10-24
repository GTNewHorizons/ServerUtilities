package serverutils.lib.icon;

import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.ServerUtilitiesConfig;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.item.ItemStackSerializer;
import serverutils.lib.util.InvUtils;

public class ItemIcon extends Icon {

    private static class LazyItemIcon extends ItemIcon {

        private String lazyStackString;
        private boolean createdStack;

        private LazyItemIcon(String s) {
            super(InvUtils.EMPTY_STACK);
            lazyStackString = s;
        }

        @Override
        public ItemStack getStack() {
            if (!createdStack) {
                stack = ItemStackSerializer.parseItem(lazyStackString);
                createdStack = true;

                if (ServerUtilitiesConfig.debugging.print_more_errors && stack == null) {
                    stack = InvUtils.brokenItem(lazyStackString);
                }
            }

            return stack;
        }

        public String toString() {
            return "item:" + lazyStackString;
        }
    }

    ItemStack stack;

    public static Icon getItemIcon(ItemStack stack) {
        return stack == null ? EMPTY : new ItemIcon(stack);
    }

    public static Icon getItemIcon(Item item) {
        return getItemIcon(new ItemStack(item));
    }

    public static Icon getItemIcon(Block block) {
        return getItemIcon(new ItemStack(block));
    }

    public static Icon getItemIcon(String lazyStackString) {
        return lazyStackString.isEmpty() ? EMPTY : new LazyItemIcon(lazyStackString);
    }

    private ItemIcon(ItemStack is) {
        stack = is;
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void draw(int x, int y, int w, int h) {
        GuiHelper.drawItem(getStack(), x, y, w / 16D, h / 16D, true);
        GuiHelper.setupDrawing();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawStatic(int x, int y, int w, int h) {
        GuiHelper.drawItem(getStack(), x, y, w / 16D, h / 16D, false);
        GuiHelper.setupDrawing();
    }

    @SideOnly(Side.CLIENT)
    public static void drawItem3D(ItemStack stack) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        // mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1F, -1F, -0.02F);
        // IBakedModel bakedmodel = RenderItem.getInstance().getItemModelWithOverrides(stack, mc.theWorld,
        // mc.thePlayer);
        // bakedmodel = ForgeHooksClient.handleCameraTransforms(bakedmodel, ItemCameraTransforms.TransformType.GUI,
        // false);
        ItemRenderer renderer = new ItemRenderer(mc);
        renderer.renderItem(mc.thePlayer, stack, 0, ItemRenderType.ENTITY);
        GlStateManager.popMatrix();
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        // mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void draw3D() {
        drawItem3D(getStack());
    }

    public String toString() {
        return "item:" + ItemStackSerializer.toString(getStack());
    }

    public int hashCode() {
        ItemStack stack = getStack();
        int h = stack.getItem().hashCode();
        h = h * 31 + stack.stackSize;
        h = h * 31 + stack.getItemDamage();
        h = h * 31 + Objects.hashCode(stack.getTagCompound());
        return h;
    }

    public boolean equals(Object o) {
        return o == this
                || o instanceof ItemIcon && ItemStack.areItemStackTagsEqual(getStack(), ((ItemIcon) o).getStack());
    }

    @Override
    @Nullable
    public Object getIngredient() {
        return getStack();
    }
}
