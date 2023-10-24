package serverutils.events.chunks;

import serverutils.events.ServerUtilitiesEvent;
import serverutils.net.MessageClaimedChunksUpdate;

public class UpdateClientDataEvent extends ServerUtilitiesEvent {

    private final MessageClaimedChunksUpdate message;

    public UpdateClientDataEvent(MessageClaimedChunksUpdate m) {
        message = m;
    }

    public MessageClaimedChunksUpdate getMessage() {
        return message;
    }
}
