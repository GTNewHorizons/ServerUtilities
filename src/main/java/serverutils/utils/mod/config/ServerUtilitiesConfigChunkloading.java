package serverutils.utils.mod.config;

import latmod.lib.annotations.Flags;
import latmod.lib.annotations.Info;
import latmod.lib.annotations.NumberBounds;
import serverutils.lib.api.config.ConfigEntryBool;
import serverutils.lib.api.config.ConfigEntryDouble;

public class ServerUtilitiesConfigChunkloading {

    @Flags(Flags.SYNC)
    @Info("Enables chunkloading")
    public static final ConfigEntryBool enabled = new ConfigEntryBool("enabled", true);

    @Flags(Flags.SYNC)
    @NumberBounds(min = -1D)
    @Info({ "Max hours player can be offline until he's chunks unload",
            "0 - Disabled, will unload instantly when he disconnects", "-1 - Chunk will always be loaded" })
    public static final ConfigEntryDouble max_player_offline_hours = new ConfigEntryDouble(
            "max_player_offline_hours",
            -1D);
}
