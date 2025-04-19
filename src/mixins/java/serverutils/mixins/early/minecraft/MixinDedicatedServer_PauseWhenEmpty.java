package serverutils.mixins.early.minecraft;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.PropertyManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import serverutils.data.IPauseWhenEmptyServerConfig;

@Mixin(DedicatedServer.class)
public class MixinDedicatedServer_PauseWhenEmpty implements IPauseWhenEmptyServerConfig {

    @Shadow
    private PropertyManager settings;

    @Unique
    private int serverUtilities$pauseWhenEmptySeconds = 0;

    @Override
    public int serverUtilities$getPauseWhenEmptySeconds() {
        return serverUtilities$pauseWhenEmptySeconds;
    }

    @Override
    public void serverUtilities$setPauseWhenEmptySeconds(int value) {
        serverUtilities$pauseWhenEmptySeconds = Math.max(value, 0);
        settings.setProperty("pause-when-empty-seconds", serverUtilities$pauseWhenEmptySeconds);
        settings.saveProperties();
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
