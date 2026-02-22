package serverutils.mixins.early.minecraft;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import serverutils.watchdog.IMaxTickTimeMinecraftServer;
import serverutils.watchdog.ServerHangWatchdog;

@SuppressWarnings("unused")
@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer_MaxTickTime implements IMaxTickTimeMinecraftServer {

    @Unique
    protected long serverutilties$currentTime = System.currentTimeMillis();

    @Override
    public long serverutilities$getCurrentTime() {
        return serverutilties$currentTime;
    }

    @Redirect(
            method = "run",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getSystemTimeMillis()J"))
    public long serverutilities$run() {
        long ret = MinecraftServer.getSystemTimeMillis();
        serverutilties$currentTime = ret;
        return ret;
    }

    @Inject(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;func_147138_a(Lnet/minecraft/network/ServerStatusResponse;)V",
                    shift = At.Shift.AFTER))
    public void serverutilities$runWatchdog(CallbackInfo ci) {
        ServerHangWatchdog.init();
    }
}
