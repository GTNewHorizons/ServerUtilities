package serverutils.lib.lib.net;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import serverutils.lib.ServerUtilitiesLib;

enum MessageToClientHandler implements IMessageHandler<MessageToClient, IMessage> {

    INSTANCE;

    @Override
    public IMessage onMessage(MessageToClient message, MessageContext context) {
        ServerUtilitiesLib.PROXY.handleClientMessage(message);
        return null;
    }
}
