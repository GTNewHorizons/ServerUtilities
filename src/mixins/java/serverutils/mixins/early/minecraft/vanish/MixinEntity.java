package serverutils.mixins.early.minecraft.vanish;

import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import serverutils.lib.util.ServerUtils;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "playSound", at = @At(value = "HEAD"), cancellable = true)
    private void serverutilities$skipIfVanished(String name, float volume, float pitch, CallbackInfo ci) {
        if (ServerUtils.isVanished((Entity) (Object) this)) ci.cancel();
    }

    @Inject(method = "updateFallState", at = @At(value = "HEAD"), cancellable = true)
    private void serverutilities$skipIfVanished(double distanceFallenThisTick, boolean isOnGround, CallbackInfo ci) {
        if (ServerUtils.isVanished((Entity) (Object) this)) ci.cancel();
    }
}
