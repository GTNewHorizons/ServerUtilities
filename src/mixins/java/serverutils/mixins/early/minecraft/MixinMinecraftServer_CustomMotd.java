package serverutils.mixins.early.minecraft;

import net.minecraft.network.ServerStatusResponse;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import serverutils.ServerUtilitiesConfig;
import serverutils.lib.util.MOTDFormatter;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer_CustomMotd {

    @Inject(method = "func_147134_at", at = @At("RETURN"))
    private void serverutilities$applyCustomMotd(CallbackInfoReturnable<ServerStatusResponse> cir) {
        if (!ServerUtilitiesConfig.motd.enabled) {
            return;
        }

        ServerStatusResponse response = cir.getReturnValue();
        if (response != null) {
            MinecraftServer server = (MinecraftServer) (Object) this;
            IChatComponent customMotd = MOTDFormatter.buildMOTD(server);
            response.func_151315_a(customMotd); // setServerMotd
        }
    }
}
