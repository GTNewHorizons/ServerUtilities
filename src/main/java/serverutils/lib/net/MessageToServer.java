package serverutils.lib.net;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.relauncher.Side;
import io.netty.channel.ChannelFutureListener;

public abstract class MessageToServer extends MessageBase {

    public final void sendToServer() {
        FMLEmbeddedChannel channel = getWrapper().getChannel(Side.CLIENT);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TOSERVER);
        channel.writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public void onMessage(EntityPlayerMP player) {}
}
