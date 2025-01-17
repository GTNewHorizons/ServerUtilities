package serverutils.mixins.early.minecraft.vanish;

import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import serverutils.lib.util.ServerUtils;

@Mixin(World.class)
public abstract class MixinWorld {

    @WrapWithCondition(
            method = "selectEntitiesWithinAABB",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/Chunk;getEntitiesOfTypeWithinAAAB(Ljava/lang/Class;Lnet/minecraft/util/AxisAlignedBB;Ljava/util/List;Lnet/minecraft/command/IEntitySelector;)V"))
    private <T> boolean serverutilities$ignoreVanishedPlayers(Chunk instance, Class<T> clazz, AxisAlignedBB aabb,
            List<T> list, IEntitySelector iEntitySelector) {
        if (EntityPlayer.class.isAssignableFrom(clazz)) {
            instance.getEntitiesOfTypeWithinAAAB(
                    clazz,
                    aabb,
                    list,
                    serverutilities$getNonVanishedSelector(iEntitySelector));
            return false;
        }
        return true;
    }

    @Unique
    private IEntitySelector serverutilities$getNonVanishedSelector(IEntitySelector original) {
        return entity -> !ServerUtils.isVanished(entity) && (original == null || original.isEntityApplicable(entity));
    }
}
