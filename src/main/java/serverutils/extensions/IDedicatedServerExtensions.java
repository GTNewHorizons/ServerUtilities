package serverutils.extensions;

import javax.annotation.Nonnull;

import net.minecraft.server.dedicated.PropertyManager;

/**
 * Extensions for a Minecraft dedicated server.
 */
public interface IDedicatedServerExtensions {

    /**
     * Get the property manager being used by the server.
     *
     * @return The property manager being used by the server.
     */
    @Nonnull
    PropertyManager getPropertyManager();
}
