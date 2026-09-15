package serverutils.mixins.early.minecraft;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import serverutils.task.backup.BackupTask;
import serverutils.task.backup.ExternalBackupHold;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_BackupShutdown {

    // Crash shutdown skips FMLServerStoppingEvent, but still calls stopServer before its final save.
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void serverutilities$stopBackup(CallbackInfo ci) {
        // An external tool may still be reading; its capture is void either way, and saving must be back on before
        // the final save runs. Released inline because the tick loop has stopped, so a dispatched release would
        // sit in the queue forever.
        ExternalBackupHold.INSTANCE.forceReleaseNow("server is shutting down");
        BackupTask.stopBackupThread();
    }
}
