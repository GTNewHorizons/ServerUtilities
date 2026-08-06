package serverutils.mixins.late.witchery;

import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.emoniph.witchery.common.GenericEvents;

@Mixin(GenericEvents.class)
public class MixinWitchery_CancelWakeUpEvents {

    @Inject(method = "onPlayerWakeUpEvent", at = @At("HEAD"), cancellable = true, remap = false)
    public void onPlayerWakeUpEvent(PlayerWakeUpEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
