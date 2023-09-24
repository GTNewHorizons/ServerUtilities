package serverutils.serverlib.lib.net;

import serverutils.serverlib.FTBLib;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * @author LatvianModder
 */
enum MessageToClientHandler implements IMessageHandler<MessageToClient, IMessage>
{
	INSTANCE;

	@Override
	public IMessage onMessage(MessageToClient message, MessageContext context)
	{
		FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() -> FTBLib.PROXY.handleClientMessage(message));
		return null;
	}
}