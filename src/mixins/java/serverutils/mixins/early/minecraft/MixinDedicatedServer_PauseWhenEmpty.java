package serverutils.mixins.early.minecraft;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.PropertyManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import serverutils.data.IPauseWhenEmpty;

@Mixin(DedicatedServer.class)
public class MixinDedicatedServer_PauseWhenEmpty implements IPauseWhenEmpty {

    @Shadow
    private PropertyManager settings;

    @Unique
    private int serverUtilities$pauseWhenEmptySeconds = 0;

    @Unique
    private int serverUtilities$pauseWhenEmptySecondsOneShot = -1;

    @Override
    public int serverUtilities$getPauseWhenEmptySeconds() {
        return serverUtilities$pauseWhenEmptySecondsOneShot > -1 ? serverUtilities$pauseWhenEmptySecondsOneShot
                : serverUtilities$pauseWhenEmptySeconds;
    }

    @Override
    public void serverUtilities$setPauseWhenEmptySeconds(int value, boolean oneshot) {
        if (oneshot) {
            serverUtilities$pauseWhenEmptySecondsOneShot = Math.max(value, -1);
        } else {
            serverUtilities$pauseWhenEmptySeconds = Math.max(value, 0);
            settings.setProperty("pause-when-empty-seconds", serverUtilities$pauseWhenEmptySeconds);
            settings.saveProperties();
        }
    }

    @Inject(
            method = "startServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lcpw/mods/fml/common/FMLCommonHandler;onServerStarted()V",
                    remap = false,
                    shift = At.Shift.AFTER))
    public void su$setupServer(CallbackInfoReturnable<Boolean> cir) {
        serverUtilities$pauseWhenEmptySeconds = settings.getIntProperty("pause-when-empty-seconds", 0);
    }
}
