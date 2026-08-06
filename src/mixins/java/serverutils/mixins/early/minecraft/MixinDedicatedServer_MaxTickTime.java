package serverutils.mixins.early.minecraft;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.PropertyManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import serverutils.watchdog.IMaxTickTimeDedicatedServer;

@Mixin(DedicatedServer.class)
@SuppressWarnings("unused")
public class MixinDedicatedServer_MaxTickTime implements IMaxTickTimeDedicatedServer {

    @Shadow
    private PropertyManager settings;
    @Unique
    private long serverutilties$maxTickTime = 0;

    @Override
    public long serverutilities$getMaxTickTime() {
        return serverutilties$maxTickTime;
    }

    @Unique
    private long serverutilties$getLongProperty(String key, long defaultValue) {
        try {
            return Long.parseLong(settings.getStringProperty(key, String.valueOf(defaultValue)));
        } catch (Exception var5) {
            settings.setProperty(key, String.valueOf(defaultValue));
            settings.saveProperties();
            return defaultValue;
        }
    }

    @Inject(
            method = "startServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lcpw/mods/fml/common/FMLCommonHandler;onServerStarted()V",
                    remap = false,
                    shift = At.Shift.AFTER))
    public void serverutilties$startServer(CallbackInfoReturnable<Boolean> cir) {
        // -1 default since it didn't exist before
        serverutilties$maxTickTime = serverutilties$getLongProperty("max-tick-time", -1);
    }

}
