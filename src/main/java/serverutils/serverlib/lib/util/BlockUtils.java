package serverutils.serverlib.lib.util;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import serverutils.serverlib.lib.math.BlockDimPos;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class BlockUtils
{
	public static final int UPDATE = 1;
	public static final int SEND_TO_CLIENTS = 2;
	public static final int NO_RERENDER = 4;
	public static final int RERENDER_MAIN_THREAD = 8;
	public static final int NO_OBSERVERS = 16;

	public static final int DEFAULT = UPDATE | SEND_TO_CLIENTS;
	public static final int DEFAULT_AND_RERENDER = DEFAULT | RERENDER_MAIN_THREAD;

	public static final IBlockState AIR_STATE = Blocks.AIR.getDefaultState();
	public static final String DATA_TAG = "BlockEntityTag";

	public static String getNameFromState(IBlockState state)
	{
		if (state == AIR_STATE)
		{
			return "minecraft:air";
		}

		StringBuilder builder = new StringBuilder();
		builder.append(Block.blockRegistry.getNameForObject(state.getBlock()));

		if (state != state.getBlock().getDefaultState() && !state.getProperties().isEmpty())
		{
			builder.append('[');
			boolean first = true;

			for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet())
			{
				if (first)
				{
					first = false;
				}
				else
				{
					builder.append(',');
				}

				builder.append(entry.getKey().getName());
				builder.append('=');
				builder.append(entry.getKey().getName(CommonUtils.cast(entry.getValue())));
			}

			builder.append(']');
		}
		return builder.toString();
	}

	public static IBlockState getStateFromName(String name, IBlockState def)
	{
		if (name.isEmpty())
		{
			return def;
		}

		int p = name.indexOf('[');
		String stateName = p == -1 ? name : name.substring(0, p);
		IBlockState state = Block.blockRegistry.getObject(new ResourceLocation(stateName)).getDefaultState();

		if (state == AIR_STATE)
		{
			return def;
		}

		if (p >= 0)
		{
			for (String property : name.substring(p + 1, name.length() - 1).split(","))
			{
				String[] p1 = property.split("=", 2);
				IProperty<?> property1 = state.getBlock().getBlockState().getProperty(p1[0]);

				if (property1 != null)
				{
					com.google.common.base.Optional<?> propValue = property1.parseValue(p1[1]);

					if (propValue.isPresent())
					{
						state = state.withProperty(property1, CommonUtils.cast(propValue.get()));
					}
				}
			}
		}

		return state;
	}

	public static IBlockState getStateFromName(String name)
	{
		return getStateFromName(name, AIR_STATE);
	}

	public static void notifyBlockUpdate(World world, BlockDimPos pos, @Nullable IBlockState state)
	{
		if (state == null)
		{
			state = world.getBlockState(pos);
		}

		world.notifyBlockUpdate(pos, state, state, DEFAULT_AND_RERENDER);
	}

	public static boolean hasData(ItemStack stack)
	{
		NBTTagCompound nbt = stack.getTagCompound();
		return nbt != null && nbt.hasKey(DATA_TAG);
	}

	public static NBTTagCompound getData(ItemStack stack)
	{
		NBTTagCompound nbt = stack.getTagCompound();
		return nbt != null ? nbt.getCompoundTag(DATA_TAG) : new NBTTagCompound();
	}

	public static void removeData(ItemStack stack)
	{
		NBTTagCompound nbt = stack.getTagCompound();

		if (nbt != null)
		{
			nbt.removeTag(DATA_TAG);
			nbt.getCompoundTag("display").removeTag("Lore");
			stack.setTagCompound(NBTUtils.minimize(nbt));
		}
	}
}
