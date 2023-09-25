package serverutils.utils.events.chunks;

import serverutils.utils.events.FTBUtilitiesEvent;
import serverutils.utils.net.MessageClaimedChunksUpdate;

/**
 * @author LatvianModder
 */
public class UpdateClientDataEvent extends FTBUtilitiesEvent {

    private final MessageClaimedChunksUpdate message;

    public UpdateClientDataEvent(MessageClaimedChunksUpdate m) {
        message = m;
    }

    public MessageClaimedChunksUpdate getMessage() {
        return message;
    }
}
