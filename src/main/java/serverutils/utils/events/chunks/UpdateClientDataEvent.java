package serverutils.utils.events.chunks;

import serverutils.utils.events.ServerUtilitiesEvent;
import serverutils.utils.net.MessageClaimedChunksUpdate;

public class UpdateClientDataEvent extends ServerUtilitiesEvent {

    private final MessageClaimedChunksUpdate message;

    public UpdateClientDataEvent(MessageClaimedChunksUpdate m) {
        message = m;
    }

    public MessageClaimedChunksUpdate getMessage() {
        return message;
    }
}
