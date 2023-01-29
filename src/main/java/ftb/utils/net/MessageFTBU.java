package ftb.utils.net;

import latmod.lib.ByteCount;
import ftb.lib.api.net.*;

abstract class MessageFTBU extends MessageLM {

    public MessageFTBU(ByteCount t) {
        super(t);
    }

    public LMNetworkWrapper getWrapper() {
        return FTBUNetHandler.NET;
    }
}
