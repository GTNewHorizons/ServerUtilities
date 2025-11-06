package serverutils.task;

import net.minecraft.util.IChatComponent;

import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.MOTDFormatter;

public class UpdateMOTDTask extends Task {

    /**
     * Create a new instance.
     */
    public UpdateMOTDTask() {
        // -- Runs every 5 seconds
        super(Ticks.getFromMillis(5000L));
    }

    /**
     * Update the MOTD text.
     *
     * @param universe The state of the universe.
     */
    @Override
    public void execute(Universe universe) {
        IChatComponent motd = MOTDFormatter.buildMOTD(universe.server);
        universe.server.func_147134_at().func_151315_a(motd);
    }
}
