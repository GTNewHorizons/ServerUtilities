package serverutils.lib.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import serverutils.lib.tile.TileBase;

public class BlockSpecialDrop extends Block {

    public BlockSpecialDrop(Material material, MapColor color) {
        super(material);
    }

    // @Override
    // @Deprecated
    // public ItemStack getItem(World world, int posx, int posy, int posz) {
    // ItemStack stack = super.getItem(world, posx, posy, posz);
    // TileEntity tileEntity = world.getTileEntity(pos);

    // if (tileEntity instanceof TileBase) {
    // ((TileBase) tileEntity).writeToPickBlock(stack);
    // }

    // return stack;
    // }

    @Override
    public void onBlockPlacedBy(World world, int posx, int posy, int posz, EntityLivingBase placer, ItemStack stack) {
        if (hasTileEntity(0)) {
            TileEntity tile = world.getTileEntity(posx, posy, posz);

            if (tile instanceof TileBase tileBase) {
                tileBase.readFromItem(stack);
            }
        }
    }

    @Override
    public void dropBlockAsItemWithChance(World world, int posx, int posy, int posz, int meta, float chance,
            int fortune) {}

    @Override
    public void onBlockHarvested(World world, int posx, int posy, int posz, int meta, EntityPlayer player) {
        if (player.capabilities.isCreativeMode) {
            TileEntity tileEntity = world.getTileEntity(posx, posy, posz);

            if (tileEntity instanceof TileBase tileBase) {
                tileBase.brokenByCreative = true;
            }
        }
    }

    // @Override
    // @SuppressWarnings("deprecation")
    // public void breakBlock(World world, int posx, int posy, int posz, Block block, int meta) {
    // ItemStack stack = super.getItem(world, posx, posy, posz);
    // TileEntity tileEntity = world.getTileEntity(pos);

    // if (tileEntity instanceof TileBase) {
    // if (((TileBase) tileEntity).brokenByCreative) {
    // return;
    // }

    // ((TileBase) tileEntity).writeToItem(stack);
    // }

    // spawnAsEntity(world, pos, stack);
    // super.breakBlock(world, pos, state);
    // }
}
