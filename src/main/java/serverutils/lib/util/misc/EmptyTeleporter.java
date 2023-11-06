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
    public void placeInPortal(Entity p_77185_1_, double p_77185_2_, double p_77185_4_, double p_77185_6_,
            float p_77185_8_) {
        p_77185_1_.motionX = p_77185_1_.motionY = p_77185_1_.motionZ = 0.0D;
        p_77185_1_.fallDistance = 0F;
        p_77185_1_.setLocationAndAngles(
                destination.posX,
                destination.posY,
                destination.posZ,
                p_77185_1_.rotationYaw,
                0.0F);
    }

    @Override
    public boolean placeInExistingPortal(Entity p_77184_1_, double p_77184_2_, double p_77184_4_, double p_77184_6_,
            float p_77184_8_) {
        return false;
    }

    @Override
    public boolean makePortal(Entity entity) {
        return false;
    }
}
