package serverutils.mixins.early.minecraft.vanish;

import static serverutils.ServerUtilitiesPermissions.SEE_VANISH;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.permission.PermissionAPI;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @Shadow
    @Final
    private MinecraftServer serverController;

    @Shadow
    public EntityPlayerMP playerEntity;

    @WrapWithCondition(
            method = "onDisconnect",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendChatMsg(Lnet/minecraft/util/IChatComponent;)V"))
    private boolean serverutilities$skipMessageIfVanished(ServerConfigurationManager instance,
            IChatComponent component) {
        if (ServerUtils.isVanished(playerEntity)) {
            serverutilities$sendMessageToAllowedPlayers(component);
            return false;
        }

        return true;
    }

    @Unique
    private void serverutilities$sendMessageToAllowedPlayers(IChatComponent component) {
        serverController.addChatMessage(component);
        for (EntityPlayerMP player : serverController.getConfigurationManager().playerEntityList) {
            if (PermissionAPI.hasPermission(player, SEE_VANISH)) {
                player.addChatMessage(component);
            }
        }
    }
}
