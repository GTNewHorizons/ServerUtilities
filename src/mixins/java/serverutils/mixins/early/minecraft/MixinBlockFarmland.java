package serverutils.mixins.early.minecraft;

import net.minecraft.block.BlockFarmland;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import serverutils.data.ClaimedChunks;

@Mixin(BlockFarmland.class)
public class MixinBlockFarmland {

    @Inject(
            method = "onFallenUpon",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIILnet/minecraft/block/Block;)Z"),
            cancellable = true)
    private void serverutilities$setPermissionNode(World worldIn, int x, int y, int z, Entity entityIn,
            float fallDistance, CallbackInfo ci) {
        if (entityIn instanceof EntityPlayer player && ClaimedChunks.blockBlockEditing(player, x, y, z, 0)) {
            ci.cancel();
        }
    }
}
