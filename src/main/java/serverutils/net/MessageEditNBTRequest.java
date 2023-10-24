package serverutils.net;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.StringJoiner;

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

        if (ray != null) {
            if (ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                ClientUtils.execClientCommand(
                        StringJoiner.with(' ').joinObjects("/nbtedit block", ray.blockX, ray.blockY, ray.blockZ));
            } else if (ray.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && ray.entityHit != null) {
                ClientUtils.execClientCommand("/nbtedit entity " + ray.entityHit.getEntityId());
            }
        }
    }
}
