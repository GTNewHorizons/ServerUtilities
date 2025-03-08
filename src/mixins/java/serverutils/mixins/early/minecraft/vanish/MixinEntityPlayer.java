package serverutils.mixins.early.minecraft.vanish;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.VanishData;
import serverutils.lib.util.ServerUtils;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase {

    public MixinEntityPlayer(World worldIn) {
        super(worldIn);
    }

    @WrapWithCondition(
            method = "collideWithPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;onCollideWithPlayer(Lnet/minecraft/entity/player/EntityPlayer;)V"))
    private boolean serverutilities$noVanishCollision(Entity instance, EntityPlayer entityIn) {
        if (ServerUtils.isVanished(entityIn)) {
            VanishData data = ServerUtilitiesPlayerData.get(entityIn).getVanishData();
            return data.collision;
        }
        return true;
    }

    @WrapWithCondition(
            method = "playSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSoundToNearExcept(Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;FF)V"))
    private boolean serverutilities$noVanishSound(World instance, EntityPlayer player, String p_85173_1_,
            float p_85173_2_, float p_85173_3_) {
        return !ServerUtils.isVanished(this);
    }

}
