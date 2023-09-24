package serverutils.serverlib.lib.block;

import serverutils.serverlib.lib.tile.TileBase;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author LatvianModder
 */
public class BlockSpecialDrop extends Block
{
	public BlockSpecialDrop(Material material, MapColor color)
	{
		super(material, color);
	}

	@Override
	@Deprecated
	public ItemStack getItem(World world, BlockPos pos, IBlockState state)
	{
		ItemStack stack = super.getItem(world, pos, state);
		TileEntity tileEntity = world.getTileEntity(pos);

		if (tileEntity instanceof TileBase)
		{
			((TileBase) tileEntity).writeToPickBlock(stack);
		}

		return stack;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
	{
		if (hasTileEntity(state))
		{
			TileEntity tile = world.getTileEntity(pos);

			if (tile instanceof TileBase)
			{
				((TileBase) tile).readFromItem(stack);
			}
		}
	}

	@Override
	public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune)
	{
	}

	@Override
	public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player)
	{
		if (player.capabilities.isCreativeMode)
		{
			TileEntity tileEntity = world.getTileEntity(pos);

			if (tileEntity instanceof TileBase)
			{
				((TileBase) tileEntity).brokenByCreative = true;
			}
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void breakBlock(World world, BlockPos pos, IBlockState state)
	{
		ItemStack stack = super.getItem(world, pos, state);
		TileEntity tileEntity = world.getTileEntity(pos);

		if (tileEntity instanceof TileBase)
		{
			if (((TileBase) tileEntity).brokenByCreative)
			{
				return;
			}

			((TileBase) tileEntity).writeToItem(stack);
		}

		spawnAsEntity(world, pos, stack);
		super.breakBlock(world, pos, state);
	}
}