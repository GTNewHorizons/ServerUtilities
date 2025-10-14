package serverutils.mixins.early.minecraft;

import static serverutils.ServerUtilitiesPermissions.BYPASS_PLAYER_LIMIT;

import net.minecraft.server.network.NetHandlerLoginServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;

import serverutils.lib.util.permission.PermissionAPI;

@Mixin(NetHandlerLoginServer.class)
public class MixinNetHandlerLoginServer {

    @Shadow
    private GameProfile field_147337_i;

    @ModifyExpressionValue(
            method = "func_147326_c",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/management/ServerConfigurationManager;allowUserToConnect(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Ljava/lang/String;"))
    private String serverutilities$skipMessageIfVanished(String original) {
        if (original == null || field_147337_i == null) {
            return original;
        }

        if (PermissionAPI.hasPermission(field_147337_i, BYPASS_PLAYER_LIMIT, null)) {
            return null;
        }

        return original;
    }
}
