package serverutils.lib.client;

import net.minecraft.client.particle.EntityReddustFX;
import net.minecraft.world.World;

public class ParticleColoredDust extends EntityReddustFX {

    public ParticleColoredDust(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, float red, float green,
            float blue, float alpha) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0F, 0F, 0F);
        setRBGColorF(red, green, blue);
        setAlphaF(alpha);
    }
}
