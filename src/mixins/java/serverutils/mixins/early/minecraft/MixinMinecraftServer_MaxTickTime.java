package serverutils.mixins.early.minecraft;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import serverutils.watchdog.IMaxTickTimeMinecraftServer;

@SuppressWarnings("unused")
@Mixin(MinecraftServer.class)
public class MixinMinecraftServer_MaxTickTime implements IMaxTickTimeMinecraftServer {

    @Unique
    protected long serverutilties$currentTime = System.currentTimeMillis();

    @Override
    public long serverutilities$getCurrentTime() {
        return serverutilties$currentTime;
    }

    @Inject(
            method = "run",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/server/MinecraftServer;getSystemTimeMillis()J",
                    ordinal = 0))
    public void serverutilities$run(CallbackInfo ci, @Local(name = "i") long i) {
        serverutilties$currentTime = i;
    }

    @Inject(
            method = "run",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/server/MinecraftServer;getSystemTimeMillis()J",
                    ordinal = 1))
    public void serverutilities$run2(CallbackInfo ci, @Local(name = "j") long j) {
        serverutilties$currentTime = j;
    }
}
