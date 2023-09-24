package serverutils.serverlib.lib.block;

import serverutils.serverlib.lib.util.misc.EnumScreenPosition;
import serverutils.serverlib.lib.util.misc.NameMap;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;

/**
 * @author LatvianModder
 */
public enum EnumHorizontalOffset implements IStringSerializable
{
	CENTER("center", EnumScreenPosition.CENTER),
	NORTH("north", EnumScreenPosition.BOTTOM),
	NORTH_EAST("north_east", EnumScreenPosition.BOTTOM_RIGHT),
	EAST("east", EnumScreenPosition.RIGHT),
	SOUTH_EAST("south_east", EnumScreenPosition.TOP_RIGHT),
	SOUTH("south", EnumScreenPosition.TOP),
	SOUTH_WEST("south_west", EnumScreenPosition.TOP_LEFT),
	WEST("west", EnumScreenPosition.LEFT),
	NORTH_WEST("north_west", EnumScreenPosition.BOTTOM_LEFT);

	public static final EnumHorizontalOffset[] VALUES = values();
	private static final EnumHorizontalOffset[] OPPOSITES = {CENTER, SOUTH, SOUTH_WEST, WEST, NORTH_WEST, NORTH, NORTH_EAST, EAST, SOUTH_EAST};
	public static final NameMap<EnumHorizontalOffset> NAME_MAP = NameMap.create(CENTER, VALUES);

	private final String name;
	public final EnumScreenPosition screenPosition;
	public final BlockPos offset;

	EnumHorizontalOffset(String n, EnumScreenPosition p)
	{
		name = n;
		screenPosition = p;
		offset = new BlockPos(p.offsetX, 0, p.offsetY);
	}

	@Override
	public String getName()
	{
		return name;
	}

	public BlockPos offset(BlockPos pos)
	{
		return pos.add(offset);
	}

	public boolean isCenter()
	{
		return this == CENTER;
	}

	public EnumHorizontalOffset opposite()
	{
		return OPPOSITES[ordinal()];
	}
}