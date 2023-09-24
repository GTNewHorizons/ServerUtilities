package serverutils.serverlib.lib.block;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * @author LatvianModder
 */
public enum EnumRotation implements IStringSerializable
{
	NORMAL("normal"),
	FACING_DOWN("down"),
	UPSIDE_DOWN("upside_down"),
	FACING_UP("up");

	public static final EnumRotation[] VALUES = values();

	private final String name;

	EnumRotation(String n)
	{
		name = n;
	}

	public int getModelRotationIndexFromFacing(EnumFacing facing)
	{
		return ordinal() << 2 | facing.getOpposite().getHorizontalIndex();
	}

	@Override
	public String getName()
	{
		return name;
	}

	public static EnumRotation getRotationFromEntity(BlockPos pos, EntityLivingBase placer)
	{
		if (MathHelper.abs((float) (placer.posX - pos.getX())) < 2F && MathHelper.abs((float) (placer.posZ - pos.getZ())) < 2F)
		{
			double d = placer.posY + placer.getEyeHeight();

			if (d - pos.getY() > 2D)
			{
				return FACING_UP;
			}
			else if (pos.getY() - d > 0D)
			{
				return FACING_DOWN;
			}
		}

		return NORMAL;
	}
}