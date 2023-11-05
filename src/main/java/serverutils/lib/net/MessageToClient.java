package serverutils.lib.net;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.channel.ChannelFutureListener;
import serverutils.lib.util.ServerUtils;

public abstract class MessageToClient extends MessageBase {

    public final void sendTo(EntityPlayerMP player) {
        if (ServerUtils.isFake(player)) {
            return;
        }

        FMLEmbeddedChannel channel = getWrapper().getChannel(Side.SERVER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
        channel.writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public final void sendTo(Iterable<EntityPlayerMP> players) {
        for (EntityPlayerMP playerMP : players) {
            sendTo(playerMP);
        }
    }

    public final void sendToAll() {
        FMLEmbeddedChannel channel = getWrapper().getChannel(Side.SERVER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALL);
        channel.writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public final void sendToDimension(int dim) {
        FMLEmbeddedChannel channel = getWrapper().getChannel(Side.SERVER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.DIMENSION);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(dim);
        channel.writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public final void sendToAllAround(int dim, int posx, int posy, int posz, double range) {
        FMLEmbeddedChannel channel = getWrapper().getChannel(Side.SERVER);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS)
                .set(new NetworkRegistry.TargetPoint(dim, posx + 0.5D, posy + 0.5D, posz + 0.5D, range));
        channel.writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public final void sendToAllTracking(NetworkRegistry.TargetPoint pos) {
        FMLEmbeddedChannel channel = getWrapper().getChannel(Side.SERVER);
        // target was changed but it's not used so... ok?
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
        channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(pos);
        channel.writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public final void sendToAllTracking(int dim, double x, double y, double z) {
        sendToAllTracking(new NetworkRegistry.TargetPoint(dim, x, y, z, 0D));
    }

    @SideOnly(Side.CLIENT)
    public void onMessage() {}
}
