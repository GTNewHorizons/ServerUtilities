package serverutils.task;

import static serverutils.ServerUtilitiesConfig.motd;

import net.minecraft.util.IChatComponent;

import serverutils.lib.data.Universe;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.MOTDFormatter;

public class UpdateMOTDTask extends Task {

    /**
     * Create a new instance.
     */
    public UpdateMOTDTask() {
        super(Ticks.getFromMillis(motd.updateFrequency));
    }

    /**
     * Update the MOTD text.
     *
     * @param universe The state of the universe.
     */
    @Override
    public void execute(Universe universe) {
        IChatComponent motdComponents = MOTDFormatter.buildMOTD(universe.server);
        universe.server.func_147134_at().func_151315_a(motdComponents);
    }

    /**
     * Handle the mod configuration being reloaded.
     */
    public void onConfigReload() {
        this.interval = motd.updateFrequency;
    }
}
