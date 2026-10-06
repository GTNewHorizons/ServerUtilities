package serverutils.mixins.early.minecraft;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import serverutils.task.backup.BackupTask;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_BackupShutdown {

    // AE2 flushes deferred spawn data in its stopping event, which may precede SU's own event handler.
    @Inject(
            method = "run",
            require = 1,
            at = @At(
                    value = "INVOKE",
                    target = "Lcpw/mods/fml/common/FMLCommonHandler;handleServerStopping()V",
                    remap = false))
    private void serverutilities$stopBackupBeforeModSaves(CallbackInfo ci) {
        BackupTask.stopBackupThread();
    }

    // Crash shutdown skips FMLServerStoppingEvent, but still calls stopServer before its final save.
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void serverutilities$stopBackup(CallbackInfo ci) {
        BackupTask.stopBackupThread();
    }
}
