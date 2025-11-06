package serverutils.mixins.early.minecraft;

import javax.annotation.Nonnull;

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.PropertyManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import serverutils.extensions.IDedicatedServerExtensions;

@Mixin(DedicatedServer.class)
public class MixinDedicatedServerExtensions implements IDedicatedServerExtensions {

    @Shadow
    private PropertyManager settings;

    @Override
    @Nonnull
    public PropertyManager getPropertyManager() {
        return this.settings;
    }
}
