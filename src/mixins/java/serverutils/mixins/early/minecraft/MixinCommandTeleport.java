package serverutils.mixins.early.minecraft;

import net.minecraft.command.server.CommandTeleport;
import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.TeleportType;
import serverutils.lib.data.Universe;
import serverutils.lib.math.BlockDimPos;

@Mixin(CommandTeleport.class)
public abstract class MixinCommandTeleport {

    @Inject(
            method = "processCommand",
            at = { @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetHandlerPlayServer;setPlayerLocation(DDDFF)V"),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/entity/player/EntityPlayerMP;setPositionAndUpdate(DDD)V") })
    private void serverutilities$backCompat(CallbackInfo ci, @Local(ordinal = 0) EntityPlayerMP player) {
        ServerUtilitiesPlayerData data = ServerUtilitiesPlayerData.get(Universe.get().getPlayer(player));
        data.setLastTeleport(TeleportType.VANILLA_TP, new BlockDimPos(player));
    }
}
