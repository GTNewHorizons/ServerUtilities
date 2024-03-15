package serverutils.lib.math;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.misc.EmptyTeleporter;

public class TeleporterDimPos {

    public final double posX, posY, posZ;
    public final int dim;

    public static TeleporterDimPos of(double x, double y, double z, int dim) {
        return new TeleporterDimPos(x, y, z, dim);
    }

    public static TeleporterDimPos of(Entity entity) {
        return new TeleporterDimPos(entity.posX, entity.posY, entity.posZ, entity.dimension);
    }

    public static TeleporterDimPos of(int posx, int posy, int posz, int dim) {
        return new TeleporterDimPos(posx + 0.5D, posy + 0.1D, posz + 0.5D, dim);
    }

    private TeleporterDimPos(double x, double y, double z, int d) {
        posX = x;
        posY = y;
        posZ = z;
        dim = d;
    }

    public BlockDimPos block() {
        return new BlockDimPos(posX, posY, posZ, dim);
    }

    public void placeEntity(World world, Entity entity, float yaw) {
        entity.motionX = entity.motionY = entity.motionZ = 0D;
        entity.fallDistance = 0F;

        if (entity instanceof EntityPlayerMP playerMP && playerMP.playerNetServerHandler != null) {
            playerMP.playerNetServerHandler.setPlayerLocation(posX, posY, posZ, yaw, entity.rotationPitch);
        } else {
            entity.setLocationAndAngles(posX, posY, posZ, yaw, entity.rotationPitch);
        }
    }

    @Nullable
    public Entity teleport(@Nullable Entity entity) {
        if (entity == null || entity.worldObj.isRemote) {
            return entity;
        }

        if (ServerUtilitiesConfig.debugging.log_teleport) {
            ServerUtilities.LOGGER.info(
                    "Teleporting '" + entity.getCommandSenderName()
                            + "' to ["
                            + posX
                            + ','
                            + posY
                            + ','
                            + posZ
                            + "] in "
                            + ServerUtils.getDimensionName(dim).getUnformattedText());
        }

        if (dim != entity.dimension) {
            MinecraftServer server = ServerUtils.getServer();
            WorldServer currentDim = server.worldServerForDimension(entity.dimension);
            WorldServer newDim = server.worldServerForDimension(dim);
            if (entity instanceof EntityPlayerMP playerMP) {
                server.getConfigurationManager()
                        .transferPlayerToDimension(playerMP, dim, new EmptyTeleporter(newDim, this));
                playerMP.playerNetServerHandler.sendPacket(
                        new S1FPacketSetExperience(
                                playerMP.experience,
                                playerMP.experienceTotal,
                                playerMP.experienceLevel));
                playerMP.sendPlayerAbilities();

                if (currentDim.provider.dimensionId == 1 && playerMP.isEntityAlive()) {
                    newDim.spawnEntityInWorld(playerMP);
                    newDim.updateEntityWithOptionalForce(playerMP, false);
                }
            } else {
                server.getConfigurationManager()
                        .transferEntityToWorld(entity, dim, currentDim, newDim, new EmptyTeleporter(newDim, this));
            }
        }

        placeEntity(entity.worldObj, entity, entity.rotationYaw);
        return entity;
    }
}
