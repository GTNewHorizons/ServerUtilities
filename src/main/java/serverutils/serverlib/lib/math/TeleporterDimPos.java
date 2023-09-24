package serverutils.serverlib.lib.math;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.ServerLibConfig;
import serverutils.serverlib.lib.util.ServerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class TeleporterDimPos implements ITeleporter
{
	public final double posX, posY, posZ;
	public final int dim;

	public static TeleporterDimPos of(double x, double y, double z, int dim)
	{
		return new TeleporterDimPos(x, y, z, dim);
	}

	public static TeleporterDimPos of(Entity entity)
	{
		return new TeleporterDimPos(entity.posX, entity.posY, entity.posZ, entity.dimension);
	}

	public static TeleporterDimPos of(BlockDimPos pos, int dim)
	{
		return new TeleporterDimPos(pos.posX + 0.5D, pos.posY + 0.1D, pos.posZ + 0.5D, dim);
	}

	private TeleporterDimPos(double x, double y, double z, int d)
	{
		posX = x;
		posY = y;
		posZ = z;
		dim = d;
	}

	public BlockDimPos block()
	{
		return new BlockDimPos(posX, posY, posZ, dim);
	}

	@Override
	public void placeEntity(World world, Entity entity, float yaw)
	{
		entity.motionX = entity.motionY = entity.motionZ = 0D;
		entity.fallDistance = 0F;

		if (entity instanceof EntityPlayerMP && ((EntityPlayerMP) entity).connection != null)
		{
			((EntityPlayerMP) entity).connection.setPlayerLocation(posX, posY, posZ, yaw, entity.rotationPitch);
		}
		else
		{
			entity.setLocationAndAngles(posX, posY, posZ, yaw, entity.rotationPitch);
		}
	}

	@Nullable
	public Entity teleport(@Nullable Entity entity)
	{
		if (entity == null || entity.worldObj.isRemote)
		{
			return entity;
		}

		if (ServerLibConfig.debugging.log_teleport)
		{
			ServerLib.LOGGER.info("Teleporting '" + entity.getCommandSenderName() + "' to [" + posX + ',' + posY + ',' + posZ + "] in " + ServerUtils.getDimensionName(dim).getUnformattedText());
		}

		if (dim != entity.dimension)
		{
			return entity.travelToDimension(dim, this); //changeDimension(dim, this);
		}

		placeEntity(entity.worldObj, entity, entity.rotationYaw);
		return entity;
	}
}