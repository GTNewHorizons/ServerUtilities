package serverutils.lib.util.misc;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

import serverutils.lib.math.TeleporterDimPos;

public class EmptyTeleporter extends Teleporter {

    private final TeleporterDimPos destination;

    public EmptyTeleporter(WorldServer worldIn, TeleporterDimPos dest) {
        super(worldIn);
        this.destination = dest;
    }

    @Override
    public void removeStalePortalLocations(long p_85189_1_) {}

    @Override
    public void placeInPortal(Entity entity, double posX, double posY, double posZ, float yaw) {
        placeInExistingPortal(entity, posX, posY, posZ, yaw);
    }

    @Override
    public boolean placeInExistingPortal(Entity entity, double posX, double posY, double posZ, float yaw) {
        entity.motionX = entity.motionY = entity.motionZ = 0.0D;
        entity.fallDistance = 0F;
        entity.setLocationAndAngles(destination.posX, destination.posY, destination.posZ, entity.rotationYaw, 0.0F);
        return true;
    }

    @Override
    public boolean makePortal(Entity entity) {
        return false;
    }
}
