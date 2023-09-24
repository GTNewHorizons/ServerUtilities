package serverutils.serverlib.lib.gui.misc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public abstract class BlockGuiSupplier
{
	public final Object mod;
	public final int id;

	public BlockGuiSupplier(Object _mod, int _id)
	{
		mod = _mod;
		id = _id;
	}

	public void open(EntityPlayer player, BlockPos pos)
	{
		player.openGui(mod, id, player.world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Nullable
	public abstract Container getContainer(EntityPlayer player, TileEntity tileEntity);

	public abstract Object getGui(Container container);
}