package serverutils.serverlib.lib.net;

import serverutils.serverlib.ServerLib;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

enum MessageToClientHandler implements IMessageHandler<MessageToClient, IMessage>
{
	INSTANCE;

	@Override
	public IMessage onMessage(MessageToClient message, MessageContext context)
	{
		FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() -> ServerLib.PROXY.handleClientMessage(message));
		return null;
	}
}