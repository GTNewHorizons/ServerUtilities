package serverutils.mixins.early.minecraft;

import net.minecraft.network.NetworkSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.ServerConfigurationManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import serverutils.ServerUtilities;
import serverutils.data.IPauseWhenEmpty;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_PauseWhenEmpty {

    @Shadow
    public abstract int getCurrentPlayerCount();

    @Shadow
    private ServerConfigurationManager serverConfigManager;

    @Shadow
    protected abstract void saveAllWorlds(boolean dontLog);

    @Shadow
    public abstract NetworkSystem func_147137_ag();

    @Shadow
    public abstract int getTickCounter();

    @Unique
    private int serverUtilities$emptyTicks = 0;
    @Unique
    private boolean serverUtilities$wasPaused = false;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, order = 9000)
    public void serverUtilities$tick(CallbackInfo ci) {
        if ((Object) this instanceof DedicatedServer ds && ds instanceof IPauseWhenEmpty p) {
            int pauseTicks = p.serverUtilities$getPauseWhenEmptySeconds() * 20;
            if (pauseTicks > 0) {
                if (this.getCurrentPlayerCount() == 0) {
                    this.serverUtilities$emptyTicks++;
                } else {
                    this.serverUtilities$emptyTicks = 0;
                }

                if (serverUtilities$emptyTicks >= pauseTicks) {
                    if (!serverUtilities$wasPaused) {
                        ServerUtilities.LOGGER.info(
                                "Server empty for {} seconds, saving and pausing",
                                p.serverUtilities$getPauseWhenEmptySeconds());
                        this.serverConfigManager.saveAllPlayerData();
                        this.saveAllWorlds(true);
                        serverUtilities$wasPaused = true;
                    }
                    // to finish saving chunks
                    net.minecraftforge.common.chunkio.ChunkIOExecutor.tick();
                    // to process new connections
                    this.func_147137_ag().networkTick();
                    // to process console commands
                    ds.executePendingCommands();
                    ci.cancel();
                    return;
                }
            }

            if (serverUtilities$wasPaused) {
                ServerUtilities.LOGGER.info("Resuming server");
                serverUtilities$wasPaused = false;
                // reset the oneshot value to -1
                p.serverUtilities$setPauseWhenEmptySeconds(-1, true);
            }
        }
    }
}
