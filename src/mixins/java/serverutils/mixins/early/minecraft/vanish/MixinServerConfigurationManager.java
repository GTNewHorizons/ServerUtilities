package serverutils.mixins.early.minecraft.vanish;

import static serverutils.ServerUtilitiesPermissions.SEE_VANISH;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;

import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.permission.PermissionAPI;

@Mixin(ServerConfigurationManager.class)
public abstract class MixinServerConfigurationManager {

    @Shadow
    @Final
    public List<EntityPlayerMP> playerEntityList;

    @Shadow
    @Final
    private MinecraftServer mcServer;

    @WrapWithCondition(
            method = "initializeConnectionToPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendChatMsg(Lnet/minecraft/util/IChatComponent;)V"))
    private boolean serverutilities$skipMessageIfVanished(ServerConfigurationManager instance, IChatComponent component,
            @Local(argsOnly = true) EntityPlayerMP playerMP) {
        if (ServerUtils.isVanished(playerMP)) {
            serverutilities$sendMessageToAllowedPlayers(component);
            return false;
        }

        return true;
    }

    @WrapWithCondition(
            method = "sendPlayerInfoToAllPlayers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendPacketToAllPlayers(Lnet/minecraft/network/Packet;)V"))
    private boolean serverutilities$skipPacketIfVanished(ServerConfigurationManager instance, Packet packet,
            @Local EntityPlayerMP player) {
        if (ServerUtils.isVanished(player)) {
            serverutilities$sendPacketToAllowedPlayers(packet);
            return false;
        }

        return true;
    }

    @WrapWithCondition(
            method = "playerLoggedIn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendPacketToAllPlayers(Lnet/minecraft/network/Packet;)V"))
    private boolean serverutilities$skipPacketIfVanished2(ServerConfigurationManager instance, Packet packet,
            @Local(argsOnly = true) EntityPlayerMP playerMP) {
        if (ServerUtils.isVanished(playerMP)) {
            serverutilities$sendPacketToAllowedPlayers(packet);
            return false;
        }

        return true;
    }

    @WrapWithCondition(
            method = "playerLoggedIn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetHandlerPlayServer;sendPacket(Lnet/minecraft/network/Packet;)V"))
    private boolean serverutilities$skipIfVanished(NetHandlerPlayServer instance, Packet packet,
            @Local(argsOnly = true) EntityPlayerMP player, @Local(ordinal = 1) EntityPlayerMP playerToSend) {
        return PermissionAPI.hasPermission(player, SEE_VANISH) || !ServerUtils.isVanished(playerToSend);
    }

    @Redirect(
            method = "getAllUsernames",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayerMP;getCommandSenderName()Ljava/lang/String;"))
    private String serverutilities$removeVanished(EntityPlayerMP player) {
        if (ServerUtils.isVanished(player)) return "";
        return player.getCommandSenderName();
    }

    @Unique
    private void serverutilities$sendPacketToAllowedPlayers(Packet packet) {
        for (EntityPlayerMP player : playerEntityList) {
            if (PermissionAPI.hasPermission(player, SEE_VANISH)) {
                player.playerNetServerHandler.sendPacket(packet);
            }
        }
    }

    @Unique
    private void serverutilities$sendMessageToAllowedPlayers(IChatComponent component) {
        mcServer.addChatMessage(component);
        for (EntityPlayerMP player : playerEntityList) {
            if (PermissionAPI.hasPermission(player, SEE_VANISH)) {
                player.addChatMessage(component);
            }
        }
    }
}
