package serverutils.lib.api.client;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.BufferUtils;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.LMColorUtils;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.TempLMPlayerFromEntity;
import serverutils.lib.TextureCoords;
import serverutils.lib.api.friends.ILMPlayer;
import serverutils.lib.api.gui.IClientActionGui;
import serverutils.lib.api.gui.callback.ClientTickCallback;
import serverutils.lib.mod.client.ServerUtilitiesLibraryRenderHandler;

@SideOnly(Side.CLIENT)
public class ServerUtilitiesLibraryClient {

    public static final Minecraft mc = FMLClientHandler.instance().getClient();
    private static final HashMap<String, ResourceLocation> cachedSkins = new HashMap<>();
    private static final ResourceLocation clickSound = new ResourceLocation("gui.button.press");
    private static float lastBrightnessX, lastBrightnessY;
    private static EntityItem entityItem;
    public static int displayW, displayH;
    public static IIcon blockNullIcon = null;

    public static int getNewBlockRenderID() {
        return RenderingRegistry.getNextAvailableRenderId();
    }

    public static void addBlockRenderer(int i, ISimpleBlockRenderingHandler r) {
        RenderingRegistry.registerBlockHandler(i, r);
    }

    public static void addItemRenderer(Item item, IItemRenderer i) {
        MinecraftForgeClient.registerItemRenderer(item, i);
    }

    public static void addItemRenderer(Block block, IItemRenderer i) {
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(block), i);
    }

    public static <T extends Entity> void addEntityRenderer(Class<T> c, Render r) {
        RenderingRegistry.registerEntityRenderingHandler(c, r);
    }

    public static void addTileRenderer(Class<? extends TileEntity> c, TileEntitySpecialRenderer r) {
        ClientRegistry.bindTileEntitySpecialRenderer(c, r);
    }

    public static ILMPlayer getClientLMPlayer() {
        ILMPlayer p = (ServerUtilitiesLib.serverUtilitiesIntegration == null) ? null
                : ServerUtilitiesLib.serverUtilitiesIntegration.getLMPlayer(mc.thePlayer);
        return (p == null) ? new TempLMPlayerFromEntity(Side.CLIENT, mc.thePlayer) : p;
    }

    public static boolean isIngame() {
        return mc.theWorld != null && mc.thePlayer != null && mc.thePlayer.worldObj != null;
    }

    public static boolean isIngameWithServerUtilities() {
        return isIngame() && ServerUtilitiesLib.serverUtilitiesIntegration != null
                && ServerUtilitiesLib.serverUtilitiesIntegration.hasClientWorld();
    }

    public static int getDim() {
        return isIngame() ? mc.theWorld.provider.dimensionId : 0;
    }

    public static UUID getUUID() {
        return mc.getSession().func_148256_e().getId();
    }

    public static void spawnPart(EntityFX e) {
        mc.effectRenderer.addEffect(e);
    }

    public static KeyBinding addKeyBinding(String name, int key, String cat) {
        KeyBinding k = new KeyBinding(name, key, cat);
        ClientRegistry.registerKeyBinding(k);
        return k;
    }

    public static void onGuiClientAction() {
        if (mc.currentScreen instanceof IClientActionGui) ((IClientActionGui) mc.currentScreen).onClientDataChanged();
    }

    public static void pushMaxBrightness() {
        lastBrightnessX = OpenGlHelper.lastBrightnessX;
        lastBrightnessY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
    }

    public static void popMaxBrightness() {
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
    }

    public static EntityPlayerSP getPlayerSP(UUID uuid) {
        // getMinecraft().getIntegratedServer().getConfigurationManager().playerEntityList

        if (uuid == null) return mc.thePlayer;

        if (mc.theWorld != null) {
            EntityPlayer ep = mc.theWorld.func_152378_a(uuid);
            if (ep != null && ep instanceof EntityPlayerSP) return (EntityPlayerSP) ep;
        }

        return null;
    }

    public static ThreadDownloadImageData getDownloadImage(ResourceLocation out, String url, ResourceLocation def,
            IImageBuffer buffer) {
        TextureManager t = mc.getTextureManager();
        ThreadDownloadImageData img = (ThreadDownloadImageData) t.getTexture(out);

        if (img == null) {
            img = new ThreadDownloadImageData(null, url, def, buffer);
            t.loadTexture(out, img);
        }

        return img;
    }

    public static void playClickSound() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(clickSound, 1F));
    }

    public static void setGLColor(int c, int a) {
        int r = LMColorUtils.getRed(c);
        int g = LMColorUtils.getGreen(c);
        int b = LMColorUtils.getBlue(c);
        GlStateManager.color(r / 255F, g / 255F, b / 255F, a / 255F);
    }

    public static void setGLColor(int c) {
        setGLColor(c, LMColorUtils.getAlpha(c));
    }

    public static ByteBuffer toByteBuffer(int pixels[], boolean alpha) {
        if (pixels == null) return null;
        ByteBuffer bb = BufferUtils.createByteBuffer(pixels.length * 4);
        byte alpha255 = (byte) 255;

        for (int p : pixels) {
            bb.put((byte) LMColorUtils.getRed(p));
            bb.put((byte) LMColorUtils.getGreen(p));
            bb.put((byte) LMColorUtils.getBlue(p));
            bb.put(alpha ? (byte) LMColorUtils.getAlpha(p) : alpha255);
        }

        bb.flip();
        return bb;
    }

    public static void execClientCommand(String s) {
        mc.ingameGUI.getChatGUI().addToSentMessages(s);
        if (ClientCommandHandler.instance.executeCommand(mc.thePlayer, s) == 0) mc.thePlayer.sendChatMessage(s);
    }

    public static ResourceLocation getSkinTexture(String username) {
        ResourceLocation r = cachedSkins.get(username);

        if (r == null) {
            r = AbstractClientPlayer.getLocationSkin(username);

            try {
                AbstractClientPlayer.getDownloadImageSkin(r, username);
                cachedSkins.put(username, r);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return r;
    }

    public static void setTexture(TextureCoords tex) {
        if (tex != null) setTexture(tex.texture);
    }

    public static void setTexture(ResourceLocation tex) {
        if (tex != null) mc.getTextureManager().bindTexture(tex);
    }

    public static void clearCachedData() {
        cachedSkins.clear();
    }

    public static boolean canRenderGui() {
        return mc.currentScreen == null || mc.currentScreen instanceof GuiChat;
    }

    public static void addClientTickCallback(ClientTickCallback e) {
        ServerUtilitiesLibraryRenderHandler.callbacks.add(e);
    }

    public static void renderItem(World w, ItemStack is) {
        if (w == null || is == null) return;
        if (entityItem == null) entityItem = new EntityItem(w);
        entityItem.worldObj = w;
        entityItem.hoverStart = 0F;
        entityItem.setEntityItemStack(is);
        RenderManager.instance.renderEntityWithPosYaw(entityItem, 0D, 0D, 0D, 0F, 0F);
    }

    public static void openGui(GuiScreen gui) {
        if (gui == null && mc.thePlayer != null) mc.thePlayer.closeScreen();
        else mc.displayGuiScreen(gui);
    }

    public static void renderGuiItem(ItemStack is, RenderItem itemRender, FontRenderer font, int x, int y) {
        if (is == null || is.getItem() == null) return;
        GlStateManager.pushAttrib();
        GlStateManager.pushMatrix();
        // GL11.glTranslatef(0F, 0F, 32F);
        GlStateManager.enableLighting();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        GlStateManager.color(1F, 1F, 1F, 1F);
        FontRenderer f = is.getItem().getFontRenderer(is);
        if (f == null) f = font;
        itemRender.renderItemAndEffectIntoGUI(f, ServerUtilitiesLibraryClient.mc.getTextureManager(), is, x, y);
        itemRender.renderItemOverlayIntoGUI(f, ServerUtilitiesLibraryClient.mc.getTextureManager(), is, x, y, null);
        GlStateManager.popMatrix();
        GlStateManager.popAttrib();
    }
}
