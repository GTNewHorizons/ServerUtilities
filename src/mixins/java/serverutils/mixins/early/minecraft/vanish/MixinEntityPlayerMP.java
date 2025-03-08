package serverutils.mixins.early.minecraft.vanish;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.authlib.GameProfile;

import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.VanishData;
import serverutils.lib.util.ServerUtils;

@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP extends EntityPlayer {

    public MixinEntityPlayerMP(World p_i45324_1_, GameProfile p_i45324_2_) {
        super(p_i45324_1_, p_i45324_2_);
    }

    @Override
    protected void collideWithEntity(Entity p_82167_1_) {
        if (ServerUtils.isVanished(this)) {
            VanishData data = ServerUtilitiesPlayerData.get(this).getVanishData();
            if (!data.collision) return;
        }
        super.collideWithEntity(p_82167_1_);
    }

    @Override
    public void applyEntityCollision(Entity entityIn) {
        if (ServerUtils.isVanished(this)) {
            VanishData data = ServerUtilitiesPlayerData.get(this).getVanishData();
            if (!data.collision) return;
        }
        super.applyEntityCollision(entityIn);
    }

    @Override
    public boolean doesEntityNotTriggerPressurePlate() {
        if (ServerUtils.isVanished(this)) {
            VanishData data = ServerUtilitiesPlayerData.get(this).getVanishData();
            return !data.collision;
        }
        return super.doesEntityNotTriggerPressurePlate();
    }

    @WrapWithCondition(
            method = "handleFalling",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;updateFallState(DZ)V"))
    private boolean serverutilities$skipIfVanished(EntityPlayer instance, double v, boolean b) {
        return !ServerUtils.isVanished(this);
    }

}
