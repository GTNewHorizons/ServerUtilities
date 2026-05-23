package serverutils.net;

import net.minecraft.entity.player.EntityPlayerMP;

public class TransferHelper {

    public static void transfer(EntityPlayerMP player, String host, int port) {
        new MessageTransfer(host, port).sendTo(player);
    }
}
