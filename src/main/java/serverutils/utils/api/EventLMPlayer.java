package serverutils.utils.api;

import cpw.mods.fml.relauncher.Side;
import serverutils.lib.api.EventLM;
import serverutils.utils.world.LMPlayer;

public abstract class EventLMPlayer extends EventLM {

    public abstract LMPlayer getPlayer();

    public abstract Side getSide();
}
