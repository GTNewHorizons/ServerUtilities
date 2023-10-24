package serverutils.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import serverutils.ServerUtilities;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.client.GlStateManager;
import serverutils.lib.gui.GuiBase;
import serverutils.lib.gui.GuiHelper;
import serverutils.lib.gui.misc.ChunkSelectorMap;
import serverutils.lib.gui.misc.GuiChunkSelectorBase;
import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.Icon;
import serverutils.lib.math.MathUtils;

public class BuiltinChunkMap extends ChunkSelectorMap {

    public static final Icon TEX_ENTITY = Icon.getIcon(ServerUtilities.MOD_ID + ":textures/gui/entity.png")
            .withColor(Color4I.WHITE.withAlpha(140));
    public static final double UV = (double) TILES_GUI / (double) TILES_TEX;

    @Override
    public void resetMap(int startX, int startZ) {
        ThreadReloadChunkSelector.reloadArea(Minecraft.getMinecraft().theWorld, startX, startZ);
    }

    @Override
    public void drawMap(GuiBase gui, int ax, int ay, int startX, int startZ) {
        ThreadReloadChunkSelector.updateTexture();
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(ThreadReloadChunkSelector.getTextureId());
        GuiHelper.drawTexturedRect(
                ax,
                ay,
                TILES_GUI * GuiChunkSelectorBase.TILE_SIZE,
                TILES_GUI * GuiChunkSelectorBase.TILE_SIZE,
                Color4I.WHITE,
                0D,
                0D,
                UV,
                UV);

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;

        int cx = MathUtils.chunk(player.posX);
        int cy = MathUtils.chunk(player.posZ);

        if (cx >= startX && cy >= startZ && cx < startX + TILES_GUI && cy < startZ + TILES_GUI) {
            double x = ((cx - startX) * 16D + MathUtils.mod(player.posX, 16D));
            double y = ((cy - startZ) * 16D + MathUtils.mod(player.posZ, 16D));

            GlStateManager.pushMatrix();
            GlStateManager.translate(
                    ax + x * GuiChunkSelectorBase.TILE_SIZE / 16D,
                    ay + y * GuiChunkSelectorBase.TILE_SIZE / 16D,
                    0D);
            GlStateManager.pushMatrix();
            GlStateManager.rotate(player.rotationYaw + 180F, 0F, 0F, 1F);
            TEX_ENTITY.draw(-8, -8, 16, 16);
            GlStateManager.popMatrix();
            ClientUtils.localPlayerHead.draw(-2, -2, 4, 4);
            GlStateManager.popMatrix();
        }
    }
}
