package serverutils.mixins.early.minecraft.vanish;

import static serverutils.ServerUtilitiesPermissions.SEE_VANISH;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;

import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.VanishData;
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

    @Inject(
            method = "processPlayerDigging",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/play/client/C07PacketPlayerDigging;func_149506_g()I",
                    ordinal = 0),
            cancellable = true)
    private void serverutilities$skipVanishDigging(C07PacketPlayerDigging packetIn, CallbackInfo ci,
            @Local WorldServer worldserver) {
        if (!ServerUtils.isVanished(playerEntity)) return;
        VanishData data = ServerUtilitiesPlayerData.get(playerEntity).getVanishData();
        int type = packetIn.func_149506_g();
        if ((type <= 2 || type == 5) && !data.interaction) {
            playerEntity.playerNetServerHandler.sendPacket(
                    new S23PacketBlockChange(
                            packetIn.func_149505_c(),
                            packetIn.func_149503_d(),
                            packetIn.func_149502_e(),
                            worldserver));
            ci.cancel();
        } else if ((type == 3 || type == 4) && !data.itemDropping) {
            ci.cancel();
        }
    }

    @Inject(
            method = "processPlayerBlockPlacement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/event/ForgeEventFactory;onPlayerInteract(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$Action;IIIILnet/minecraft/world/World;)Lnet/minecraftforge/event/entity/player/PlayerInteractEvent;",
                    remap = false),
            cancellable = true)
    private void serverutilities$skipVanishItemUse(C08PacketPlayerBlockPlacement packetIn, CallbackInfo ci,
            @Local WorldServer worldserver) {
        if (!ServerUtils.isVanished(playerEntity)) return;
        VanishData data = ServerUtilitiesPlayerData.get(playerEntity).getVanishData();
        if (!data.interaction) ci.cancel();
    }

    @WrapWithCondition(
            method = "processUseEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayerMP;attackTargetEntityWithCurrentItem(Lnet/minecraft/entity/Entity;)V"))
    private boolean serverutilities$skipAttackIfVanish(EntityPlayerMP instance, Entity entity) {
        if (ServerUtils.isVanished(playerEntity)) {
            VanishData data = ServerUtilitiesPlayerData.get(playerEntity).getVanishData();
            return data.damageOthers;
        }

        return true;
    }

    @WrapWithCondition(
            method = "processUseEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayerMP;interactWith(Lnet/minecraft/entity/Entity;)Z"))
    private boolean serverutilities$skipVanishInteract(EntityPlayerMP instance, Entity entity) {
        if (ServerUtils.isVanished(playerEntity)) {
            VanishData data = ServerUtilitiesPlayerData.get(playerEntity).getVanishData();
            return data.interaction;
        }

        return true;
    }

    @WrapWithCondition(
            method = "processChatMessage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/management/ServerConfigurationManager;sendChatMsgImpl(Lnet/minecraft/util/IChatComponent;Z)V"))
    private boolean serverutilities$skipMessageIfVanished(ServerConfigurationManager instance, IChatComponent component,
            boolean isChat) {
        if (ServerUtils.isVanished(playerEntity)) {
            VanishData data = ServerUtilitiesPlayerData.get(playerEntity).getVanishData();
            if (!data.chat) {
                playerEntity.addChatMessage(new ChatComponentText("Chatting while vanished is disabled."));
                return false;
            }
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
