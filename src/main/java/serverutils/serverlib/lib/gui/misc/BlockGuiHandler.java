package serverutils.serverlib.lib.gui.misc;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import serverutils.serverlib.lib.math.BlockDimPos;

import javax.annotation.Nullable;

public class BlockGuiHandler implements IGuiHandler
{
	private final Int2ObjectOpenHashMap<BlockGuiSupplier> map = new Int2ObjectOpenHashMap<>();

	public void add(BlockGuiSupplier h)
	{
		map.put(h.id, h);
	}

	@Nullable
	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
	{
		BlockGuiSupplier supplier = map.get(ID);

		if (supplier != null)
		{
			TileEntity tileEntity = world.getTileEntity(new BlockDimPos(x, y, z, ID));

			if (tileEntity != null)
			{
				return supplier.getContainer(player, tileEntity);
			}
		}

		return null;
	}

	@Nullable
	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
	{
		BlockGuiSupplier supplier = map.get(ID);

		if (supplier != null)
		{
			TileEntity tileEntity = world.getTileEntity(new BlockDimPos(x, y, z, ID));

			if (tileEntity != null)
			{
				Container container = supplier.getContainer(player, tileEntity);
				return container == null ? null : supplier.getGui(container);
			}
		}

		return null;
	}
}