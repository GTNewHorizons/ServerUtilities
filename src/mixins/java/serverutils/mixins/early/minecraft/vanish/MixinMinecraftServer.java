package serverutils.mixins.early.minecraft.vanish;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import serverutils.lib.data.Universe;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @ModifyReturnValue(method = "getCurrentPlayerCount", at = @At(value = "RETURN"))
    private int serverutilities$dontCountVanishedPlayers(int original) {
        Universe universe = Universe.getNullable();
        if (universe == null) return original;
        return original - universe.getVanishedPlayers().size();
    }
}
