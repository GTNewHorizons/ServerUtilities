package serverutils.mixins.early.minecraft.vanish;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import serverutils.data.ServerUtilitiesPlayerData;
import serverutils.data.VanishData;
import serverutils.lib.util.ServerUtils;

@Mixin(ItemInWorldManager.class)
public abstract class MixinItemInWorldManager {

    @WrapOperation(
            method = "activateBlockOrUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;onBlockActivated(Lnet/minecraft/world/World;IIILnet/minecraft/entity/player/EntityPlayer;IFFF)Z"))
    private boolean serverutilities$vanishReadOnly(Block instance, World worldIn, int x, int y, int z,
            EntityPlayer player, int side, float subX, float subY, float subZ, Operation<Boolean> original) {
        if (ServerUtils.isVanished(player)) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            if (instance instanceof BlockContainer) {
                boolean result = original.call(instance, worldIn, x, y, z, player, side, subX, subY, subZ);
                // gives read-only access to the container and doesn't show it as open for other players
                VanishData data = ServerUtilitiesPlayerData.get(playerMP).getVanishData();
                if (result && data.containerReadOnly) {
                    playerMP.closeContainer();
                }
                return result;
            }
            return false;
        }

        return original.call(instance, worldIn, x, y, z, player, side, subX, subY, subZ);
    }

    @WrapOperation(
            method = "activateBlockOrUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;tryPlaceItemIntoWorld(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIIIFFF)Z"))
    private boolean serverutilities$skipVanishInteract(ItemStack instance, EntityPlayer p_77943_1_, World p_77943_2_,
            int p_77943_3_, int p_77943_4_, int p_77943_5_, int p_77943_6_, float p_77943_7_, float p_77943_8_,
            float p_77943_9_, Operation<Boolean> original) {
        if (ServerUtils.isVanished(p_77943_1_)) {
            VanishData data = ServerUtilitiesPlayerData.get(p_77943_1_).getVanishData();
            if (!data.interaction) return false;
        }

        return original.call(
                instance,
                p_77943_1_,
                p_77943_2_,
                p_77943_3_,
                p_77943_4_,
                p_77943_5_,
                p_77943_6_,
                p_77943_7_,
                p_77943_8_,
                p_77943_9_);
    }
}
