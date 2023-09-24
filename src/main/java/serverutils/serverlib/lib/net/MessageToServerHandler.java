package serverutils.serverlib.lib.net;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.ServerLibConfig;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

enum MessageToServerHandler implements IMessageHandler<MessageToServer, IMessage>
{
	INSTANCE;

	@Override
	public IMessage onMessage(MessageToServer message, MessageContext context)
	{
		FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() ->
		{
			if (ServerLibConfig.debugging.log_network)
			{
				ServerLib.LOGGER.info("Net TX: " + message.getClass().getName());
			}

			message.onMessage(context.getServerHandler().playerEntity);
		});

		return null;
	}
}