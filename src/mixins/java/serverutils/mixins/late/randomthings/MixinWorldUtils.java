package serverutils.mixins.late.randomthings;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import lumien.randomthings.Library.WorldUtils;
import serverutils.lib.util.ServerUtils;

@Mixin(WorldUtils.class)
public class MixinWorldUtils {

    @ModifyExpressionValue(
            method = "isPlayerOnline",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/management/ServerConfigurationManager;func_152612_a(Ljava/lang/String;)Lnet/minecraft/entity/player/EntityPlayerMP;"),
            remap = false)
    private static EntityPlayerMP serverutilities$dontShowVanished(EntityPlayerMP original) {
        if (ServerUtils.isVanished(original)) return null;
        return original;
    }
}
