package serverutils.utils.net;

import latmod.lib.ByteCount;
import serverutils.lib.api.net.*;

abstract class MessageServerUtilities extends MessageLM {

    public MessageServerUtilities(ByteCount t) {
        super(t);
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.NET;
    }
}
