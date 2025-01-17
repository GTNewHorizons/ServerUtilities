package serverutils.mixins.early.minecraft.vanish;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import serverutils.ServerUtilitiesPermissions;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.permission.PermissionAPI;

@Mixin(EntityTrackerEntry.class)
public abstract class MixinEntityTrackerEntry {

    @Shadow
    public Entity myEntity;

    @Inject(
            method = "tryStartWachingThis",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"),
            cancellable = true)
    private void serverutilities$skipIfVanished(EntityPlayerMP player, CallbackInfo ci) {
        if (ServerUtils.isVanished(myEntity)
                && !PermissionAPI.hasPermission(player, ServerUtilitiesPermissions.SEE_VANISH)) {
            ci.cancel();
        }
    }
}
