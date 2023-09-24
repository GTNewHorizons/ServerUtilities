package serverutils.serverlib.lib.gui.misc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import serverutils.serverlib.lib.math.BlockDimPos;

import javax.annotation.Nullable;

public abstract class BlockGuiSupplier
{
	public final Object mod;
	public final int id;

	public BlockGuiSupplier(Object _mod, int _id)
	{
		mod = _mod;
		id = _id;
	}

	public void open(EntityPlayer player, BlockDimPos pos)
	{
		player.openGui(mod, id, player.worldObj, pos.posX, pos.posY, pos.posZ);
	}

	@Nullable
	public abstract Container getContainer(EntityPlayer player, TileEntity tileEntity);

	public abstract Object getGui(Container container);
}