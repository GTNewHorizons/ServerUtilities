package serverutils.lib.net;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;

enum MessageToServerHandler implements IMessageHandler<MessageToServer, IMessage> {

    INSTANCE;

    @Override
    public IMessage onMessage(MessageToServer message, MessageContext context) {
        if (ServerUtilitiesConfig.debugging.log_network) {
            ServerUtilities.LOGGER.info("Net TX: " + message.getClass().getName());
        }

        message.onMessage(context.getServerHandler().playerEntity);

        return null;
    }
}
