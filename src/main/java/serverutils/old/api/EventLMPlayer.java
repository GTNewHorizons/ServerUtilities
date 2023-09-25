package serverutils.old.api;

import cpw.mods.fml.relauncher.Side;
import serverutils.lib.api.EventLM;
import serverutils.old.world.LMPlayer;

public abstract class EventLMPlayer extends EventLM {

    public abstract LMPlayer getPlayer();

    public abstract Side getSide();
}
