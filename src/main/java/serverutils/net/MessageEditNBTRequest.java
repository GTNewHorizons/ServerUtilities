package serverutils.net;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageEditNBTRequest extends MessageToClient {

    public MessageEditNBTRequest() {}

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.FILES;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        editNBT();
    }

    @SideOnly(Side.CLIENT)
    public static void editNBT() {
        MovingObjectPosition ray = Minecraft.getMinecraft().objectMouseOver;
        if (ray == null) {
            return;
        }
        if (ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && Minecraft.getMinecraft().theWorld.getTileEntity(ray.blockX, ray.blockY, ray.blockZ) != null) {
            ClientUtils.execClientCommand(String.format("/nbtedit block %d %d %d", ray.blockX, ray.blockY, ray.blockZ));
        } else if (ray.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && ray.entityHit != null) {
            ClientUtils.execClientCommand(String.format("/nbtedit entity %s", ray.entityHit.getEntityId()));
        } else if (Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem() != null) {
            ClientUtils.execClientCommand("/nbtedit item");
        } else {
            ClientUtils.execClientCommand("/nbtedit me");
        }
    }
}
