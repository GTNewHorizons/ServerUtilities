package serverutils.mixins.early.minecraft;

import net.minecraft.block.Block;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import serverutils.ServerUtilitiesConfig;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesTeamData;
import serverutils.lib.math.ChunkDimPos;

@Mixin(EntityEnderman.class)
public abstract class MixinEndermanGriefing extends EntityMob {

    public MixinEndermanGriefing(World p_i1738_1_) {
        super(p_i1738_1_);
    }

    @WrapOperation(
            method = "onLivingUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;renderAsNormalBlock()Z"))
    private boolean checkEndermanBlockPlace(Block instance, Operation<Boolean> original, @Local(ordinal = 0) int k,
            @Local(ordinal = 1) int i, @Local(ordinal = 2) int j) {
        if (!ClaimedChunks.isActive() || ServerUtilitiesConfig.world.enable_endermen.isTrue()) {
            return original.call(instance);
        }
        ClaimedChunk chunk = ClaimedChunks.instance
                .getChunk(new ChunkDimPos(k, i, j, this.worldObj.provider.dimensionId));
        if (chunk == null) {
            return original.call(instance);
        }
        ServerUtilitiesTeamData data = chunk.getData();
        if (data == null) {
            return original.call(instance);
        }
        if (data.forbidsEndermanGriefing()) {
            return false;
        }
        return original.call(instance);
    }

    @WrapOperation(
            method = "onLivingUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/monster/EntityEnderman;getCarriable(Lnet/minecraft/block/Block;)Z"))
    private boolean checkEndermanBlockGrab(Block instance, Operation<Boolean> original, @Local(ordinal = 0) int k,
            @Local(ordinal = 1) int i, @Local(ordinal = 2) int j) {
        if (!ClaimedChunks.isActive() || ServerUtilitiesConfig.world.enable_endermen.isTrue()) {
            return original.call(instance);
        }
        ClaimedChunk chunk = ClaimedChunks.instance
                .getChunk(new ChunkDimPos(k, i, j, this.worldObj.provider.dimensionId));
        if (chunk == null) {
            return original.call(instance);
        }
        ServerUtilitiesTeamData data = chunk.getData();
        if (data == null) {
            return original.call(instance);
        }
        if (data.forbidsEndermanGriefing()) {
            return false;
        }
        return original.call(instance);
    }
}
