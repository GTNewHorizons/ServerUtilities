package serverutils.serverlib.lib.net;

import serverutils.serverlib.lib.math.BlockDimPos;
import serverutils.serverlib.lib.util.ServerUtils;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.entity.player.EntityPlayerMP;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author LatvianModder
 */
public abstract class MessageToClient extends MessageBase
{
	public final void sendTo(EntityPlayerMP player)
	{
		if (ServerUtils.isFake(player))
		{
			return;
		}

		FMLEmbeddedChannel channel = getWrapper().getChannel(Side.SERVER);
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
		channel.writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
	}

	public final void sendTo(Iterable<EntityPlayerMP> players)
	{
		for (EntityPlayerMP playerMP : players)
		{
			sendTo(playerMP);
		}
	}

	public final void sendToAll()
	{
		FMLEmbeddedChannel channel = getWrapper().getChannel(Side.SERVER);
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALL);
		channel.writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
	}

	public final void sendToDimension(int dim)
	{
		FMLEmbeddedChannel channel = getWrapper().getChannel(Side.SERVER);
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.DIMENSION);
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(dim);
		channel.writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
	}

	public final void sendToAllAround(int dim, BlockDimPos pos, double range)
	{
		FMLEmbeddedChannel channel = getWrapper().getChannel(Side.SERVER);
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(new NetworkRegistry.TargetPoint(dim, pos.getBlockPos().posX + 0.5D, pos.getBlockPos().posY + 0.5D, pos.getBlockPos().posZ + 0.5D, range));
		channel.writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
	}

	public final void sendToAllTracking(NetworkRegistry.TargetPoint pos)
	{
		FMLEmbeddedChannel channel = getWrapper().getChannel(Side.SERVER);
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TRACKING_POINT);
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(pos);
		channel.writeAndFlush(this).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
	}

	public final void sendToAllTracking(int dim, double x, double y, double z)
	{
		sendToAllTracking(new NetworkRegistry.TargetPoint(dim, x, y, z, 0D));
	}

	@SideOnly(Side.CLIENT)
	public void onMessage()
	{
	}
}