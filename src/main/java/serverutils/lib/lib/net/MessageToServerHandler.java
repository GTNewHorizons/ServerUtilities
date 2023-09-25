package serverutils.lib.lib.net;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilitiesLibConfig;

enum MessageToServerHandler implements IMessageHandler<MessageToServer, IMessage> {

	INSTANCE;

	@Override
	public IMessage onMessage(MessageToServer message, MessageContext context) {
		if (ServerUtilitiesLibConfig.debugging.log_network) {
			ServerUtilitiesLib.LOGGER.info("Net TX: " + message.getClass().getName());
		}

		message.onMessage(context.getServerHandler().playerEntity);

		return null;
	}
}