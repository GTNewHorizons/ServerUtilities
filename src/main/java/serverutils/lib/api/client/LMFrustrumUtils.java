package serverutils.lib.api.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.client.renderer.entity.RenderManager;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import latmod.lib.LMUtils;

@SideOnly(Side.CLIENT)
public class LMFrustrumUtils {

    public static boolean isFirstPerson;
    public static double playerX, playerY, playerZ;
    public static double renderX, renderY, renderZ;
    public static final Frustrum frustum = new Frustrum();
    public static long playerPosHash;

    /*
     * public static final IntBuffer viewport = BufferUtils.createIntBuffer(4); public static final FloatBuffer
     * modelView = BufferUtils.createFloatBuffer(16); public static final FloatBuffer projection =
     * BufferUtils.createFloatBuffer(16);
     */

    public static void update() {
        Minecraft mc = ServerUtilitiesLibraryClient.mc;
        isFirstPerson = ServerUtilitiesLibraryClient.mc.gameSettings.thirdPersonView == 0;
        playerX = RenderManager.instance.viewerPosX;
        playerY = RenderManager.instance.viewerPosY;
        playerZ = RenderManager.instance.viewerPosZ;
        renderX = RenderManager.instance.renderPosX;
        renderY = RenderManager.instance.renderPosY;
        renderZ = RenderManager.instance.renderPosZ;
        playerPosHash = Math
                .abs(LMUtils.longHashCode(ServerUtilitiesLibraryClient.getDim(), playerX, playerY, playerZ) + 1);
        frustum.setPosition(playerX, playerY, playerZ);
    }

    /*
     * public static void updateMatrix() { GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
     * GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection); GL11.glGetInteger(GL11.GL_VIEWPORT, viewport); } public
     * static Point2D getScreenCoords(double x, double y, double z) { FloatBuffer screenCoords =
     * BufferUtils.createFloatBuffer(3); //FloatBuffer screenCoords = BufferUtils.createFloatBuffer(4); boolean result =
     * GLU.gluProject((float)x, (float)y, (float)z, modelView, projection, viewport, screenCoords); if(result) { float
     * px = screenCoords.get(0); float py = screenCoords.get(1) - screenCoords.get(2); //if(Minecraft == 0)
     * System.out.println(px + " : " + py); if(px >= 0 && py >= 0 && px < viewport.get(2) && py < viewport.get(3))
     * return new Point2D(px, py); } return null; }
     */
}
