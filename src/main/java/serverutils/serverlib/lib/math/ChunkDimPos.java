package serverutils.serverlib.lib.math;

import net.minecraft.entity.Entity;
import net.minecraft.util.ChunkCoordinates; // ChunkPos;

public final class ChunkDimPos
{
	public final int posX, posZ, dim;

	public ChunkDimPos(int x, int z, int d)
	{
		posX = x;
		posZ = z;
		dim = d;
	}
	public ChunkDimPos(ChunkCoordinates pos, int d)
	{
		this(pos.posX, pos.posZ, d);
	}

	public ChunkDimPos(BlockDimPos pos, int d)
	{
		this(MathUtils.chunk(pos.posX), MathUtils.chunk(pos.posZ), d);
	}

	public ChunkDimPos(Entity entity)
	{
		this(MathUtils.chunk(entity.posX), MathUtils.chunk(entity.posZ), entity.worldObj.provider.dimensionId);
	}

	public boolean equals(Object o)
	{
		if (o == null)
		{
			return false;
		}
		else if (o == this)
		{
			return true;
		}
		else if (o instanceof ChunkDimPos)
		{
			return equalsChunkDimPos((ChunkDimPos) o);
		}
		return false;
	}

	public boolean equalsChunkDimPos(ChunkDimPos p)
	{
		return p == this || (p.dim == dim && p.posX == posX && p.posZ == posZ);
	}

	public String toString()
	{
		return "[" + dim + '@' + posX + ',' + posZ + ']';
	}

	public int hashCode()
	{
		return 31 * (31 * posX + posZ) + dim;
	}

	public ChunkDimPos getChunkPos()
	{
		return new ChunkDimPos(posX, posZ, dim);
	}

	public int getBlockX()
	{
		return (posX << 4) + 8;
	}

	public int getBlockZ()
	{
		return (posZ << 4) + 8;
	}

	public BlockDimPos getBlockPos(int y)
	{
		return new BlockDimPos(getBlockX(), y, getBlockZ(), dim);
	}
}