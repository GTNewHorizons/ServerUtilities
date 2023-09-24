package serverutils.serverlib.lib.math;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;

public interface ITeleporter {
    void placeEntity(World var1, Entity var2, float var3);

    default boolean isVanilla() {
        return this.getClass() == Teleporter.class;
    }
}
