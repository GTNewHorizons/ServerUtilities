package serverutils.mixins.late.brigadier;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.tree.CommandNode;

import cpw.mods.fml.common.eventhandler.Event;
import serverutils.ranks.ICommandWithPermission;
import serverutils.ranks.Ranks;

@Mixin(value = CommandNode.class, remap = false)
public abstract class MixinCommandNode<S> {

    @ModifyReturnValue(method = "canUse", at = @At(value = "RETURN"), remap = false)
    private boolean serverutilities$brigadierExecute(boolean original, @Local(argsOnly = true) S source) {
        if (!(source instanceof EntityPlayerMP player) || !(this instanceof ICommandWithPermission permission))
            return original;
        Event.Result result = Ranks.INSTANCE
                .getPermissionResult(player, permission.serverutilities$getPermissionNode(), true);
        if (result == Event.Result.DEFAULT) {
            return original;
        }

        return result == Event.Result.ALLOW;
    }
}
