package serverutils.mixins.early.forge;

import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraftforge.client.GuiIngameForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import serverutils.client.gui.misc.GuiPlayerInfoWrapper;

@Mixin(GuiIngameForge.class)
public abstract class MixinGuiIngameForge {

    @ModifyExpressionValue(
            method = "renderPlayerList",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/scoreboard/ScorePlayerTeam;formatPlayerName(Lnet/minecraft/scoreboard/Team;Ljava/lang/String;)Ljava/lang/String;"))
    private String serverutilities$getDisplayName(String original, @Local GuiPlayerInfo player) {
        if (player instanceof GuiPlayerInfoWrapper wrapper) {
            return wrapper.displayName;
        }
        return original;
    }
}
