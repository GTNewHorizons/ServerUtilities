package serverutils.old.net;

import latmod.lib.ByteCount;
import serverutils.lib.api.net.LMNetworkWrapper;
import serverutils.lib.api.net.MessageLM;

abstract class MessageServerUtilities extends MessageLM {

    public MessageServerUtilities(ByteCount t) {
        super(t);
    }

    public LMNetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.NET;
    }
}
