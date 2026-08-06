package serverutils.mixins.early.minecraft;

import static serverutils.ServerUtilitiesPermissions.BYPASS_PLAYER_LIMIT;
import static serverutils.ServerUtilitiesPermissions.BYPASS_WHITELIST;

import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.server.network.NetHandlerLoginServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;

import serverutils.lib.util.permission.PermissionAPI;

@Mixin(NetHandlerLoginServer.class)
public class MixinNetHandlerLoginServer {

    /// Return value of {@link ServerConfigurationManager#allowUserToConnect(java.net.SocketAddress, GameProfile)} when
    /// the server is full.
    @Unique
    private static final String SERVER_FULL = "The server is full!";

    /// Return value of {@link ServerConfigurationManager#allowUserToConnect(java.net.SocketAddress, GameProfile)} when
    /// the player is not white-listed.
    @Unique
    private static final String NOT_WHITELISTED = "You are not white-listed on this server!";

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

        if (SERVER_FULL.equals(original) && PermissionAPI.hasPermission(field_147337_i, BYPASS_PLAYER_LIMIT, null)) {
            return null;
        }

        if (NOT_WHITELISTED.equals(original) && PermissionAPI.hasPermission(field_147337_i, BYPASS_WHITELIST, null)) {
            return null;
        }

        return original;
    }
}
