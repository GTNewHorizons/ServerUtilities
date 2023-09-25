package serverutils.lib.lib.util;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

public interface ITeleporter {

    /**
     * Called to handle placing the entity in the new world.
     *
     * The initial position of the entity will be its position in the origin world, multiplied horizontally by the
     * computed cross-dimensional movement factor (see {@link WorldProvider#getMovementFactor()}).
     *
     * Note that the supplied entity has not yet been spawned in the destination world at the time.
     *
     * @param world  the entity's destination
     * @param entity the entity to be placed
     * @param yaw    the suggested yaw value to apply
     */
    void placeEntity(World world, Entity entity, float yaw);

    // used internally to handle vanilla hardcoding
    default boolean isVanilla() {
        return false;
    }
}
