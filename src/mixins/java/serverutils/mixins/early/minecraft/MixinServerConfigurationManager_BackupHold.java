package serverutils.mixins.early.minecraft;

import java.net.SocketAddress;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.world.storage.IPlayerFileData;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import serverutils.task.backup.ExternalBackupHold;

@Mixin(ServerConfigurationManager.class)
public abstract class MixinServerConfigurationManager_BackupHold {

    @Shadow
    private IPlayerFileData playerNBTManagerObj;

    @Shadow
    @Final
    private Map<UUID, StatisticsFile> field_148547_k;

    @Inject(method = "writePlayerData", at = @At("HEAD"), cancellable = true)
    private void serverutilities$deferPlayerSave(EntityPlayerMP player, CallbackInfo ci) {
        if (player.playerNetServerHandler == null) return;
        IPlayerFileData saver = playerNBTManagerObj;
        StatisticsFile stats = field_148547_k.get(player.getUniqueID());
        if (ExternalBackupHold.INSTANCE.deferPlayerWrite(player.getUniqueID(), () -> {
            saver.writePlayerData(player);
            if (stats != null) stats.func_150883_b();
        })) ci.cancel();
    }

    @Inject(method = "allowUserToConnect", at = @At("HEAD"), cancellable = true)
    private void serverutilities$rejectReconnectDuringHold(SocketAddress address, GameProfile profile,
            CallbackInfoReturnable<String> cir) {
        if (ExternalBackupHold.INSTANCE.hasDeferredPlayerWrite(profile.getId())) {
            cir.setReturnValue("Backup in progress; reconnect when it finishes.");
        }
    }
}
