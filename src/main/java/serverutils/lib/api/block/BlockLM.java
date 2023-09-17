package serverutils.lib.api.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.LMMod;
import serverutils.lib.api.tile.TileLM;

public abstract class BlockLM extends Block implements IBlockLM {

    public final String blockName;

    public BlockLM(String s, Material m) {
        super(m);
        blockName = s;
        setBlockName(getMod().getBlockName(s));
        setHardness(1.8F);
        setResistance(3F);
        isBlockContainer = false;
    }

    public Class<? extends ItemBlock> getItemBlock() {
        return ItemBlockLM.class;
    }

    public abstract LMMod getMod();

    @SideOnly(Side.CLIENT)
    public abstract CreativeTabs getCreativeTabToDisplayOn();

    public final String getItemID() {
        return blockName;
    }

    public String getUnlocalizedName() {
        return getMod().getBlockName(blockName);
    }

    public void onPostLoaded() {}

    public int damageDropped(int meta) {
        return meta;
    }

    public boolean hasTileEntity(int meta) {
        return isBlockContainer;
    }

    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs c, List l) {
        l.add(new ItemStack(item, 1, 0));
    }

    public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase el, ItemStack is) {
        super.onBlockPlacedBy(w, x, y, z, el, is);

        if (hasTileEntity(w.getBlockMetadata(x, y, z)) && el instanceof EntityPlayer) {
            TileLM tile = getTile(w, x, y, z);
            if (tile != null) tile.onPlacedBy((EntityPlayer) el, is);
        }
    }

    public float getPlayerRelativeBlockHardness(EntityPlayer ep, World w, int x, int y, int z) {
        if (hasTileEntity(w.getBlockMetadata(x, y, z))) {
            TileLM tile = getTile(w, x, y, z);
            if (tile != null && !tile.isMinable(ep)) return -1F;
        }

        return super.getPlayerRelativeBlockHardness(ep, w, x, y, z);
    }

    public float getBlockHardness(World w, int x, int y, int z) {
        if (hasTileEntity(w.getBlockMetadata(x, y, z))) {
            TileLM tile = getTile(w, x, y, z);
            if (tile != null && !tile.isMinable(null)) return -1F;
        }

        return super.getBlockHardness(w, x, y, z);
    }

    public float getExplosionResistance(Entity entity, World w, int x, int y, int z, double explosionX,
            double explosionY, double explosionZ) {
        if (hasTileEntity(w.getBlockMetadata(x, y, z))) {
            TileLM tile = getTile(w, x, y, z);
            if (tile != null && tile.isExplosionResistant()) return 1000000F;
        }

        return super.getExplosionResistance(entity, w, x, y, z, explosionX, explosionY, explosionZ);
    }

    public int getMobilityFlag() {
        return isBlockContainer ? 2 : 0;
    }

    public void breakBlock(World w, int x, int y, int z, Block block, int meta) {
        if (!w.isRemote && hasTileEntity(w.getBlockMetadata(x, y, z))) {
            TileLM tile = getTile(w, x, y, z);
            if (tile != null) tile.onBroken();
        }
        super.breakBlock(w, x, y, z, block, meta);
    }

    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer ep, int s, float x1, float y1,
            float z1) {
        if (!hasTileEntity(w.getBlockMetadata(x, y, z))) return false;
        TileLM tile = getTile(w, x, y, z);
        return (tile != null) ? tile.onRightClick(ep, ep.getHeldItem(), s, x1, y1, z1) : false;
    }

    public boolean onBlockEventReceived(World w, int x, int y, int z, int eventID, int param) {
        if (hasTileEntity(w.getBlockMetadata(x, y, z))) {
            TileLM t = getTile(w, x, y, z);
            if (t != null) return t.receiveClientEvent(eventID, param);
        }

        return false;
    }

    public boolean recolourBlock(World w, int x, int y, int z, ForgeDirection side, int color) {
        if (hasTileEntity(w.getBlockMetadata(x, y, z))) {
            TileLM t = getTile(w, x, y, z);
            if (t != null) {
                if (t.recolorBlock(side.ordinal(), color));
                return true;
            }
        }

        return super.recolourBlock(w, x, y, z, side, color);
    }

    public void loadRecipes() {}

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack is, EntityPlayer ep, List<String> l, boolean b) {}

    public void onNeighborBlockChange(World w, int x, int y, int z, Block neighbor) {
        if (hasTileEntity(w.getBlockMetadata(x, y, z))) {
            TileLM t = getTile(w, x, y, z);
            if (t != null) t.onNeighborBlockChange(neighbor);
        }
    }

    public final Item getItem() {
        return Item.getItemFromBlock(this);
    }

    public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
        setBlockBoundsBasedOnState(w, x, y, z);
        return super.getCollisionBoundingBoxFromPool(w, x, y, z);
    }

    public int onBlockPlaced(World worldIn, int x, int y, int z, int facing, float hitX, float hitY, float hitZ,
            int meta) {
        return meta;
    }

    public static TileLM getTile(IBlockAccess w, int x, int y, int z) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (te != null && !te.isInvalid() && te instanceof TileLM) return ((TileLM) te);
        return null;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBlockIcon() {
        return blockIcon;
    }

    @SideOnly(Side.CLIENT)
    protected final String getTextureName() {
        if (textureName == null) return getMod().lowerCaseModID + ":" + blockName;
        return getMod().lowerCaseModID + ":" + textureName;
    }

    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister ir) {
        blockIcon = ir.registerIcon(getTextureName());
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int s, int m) {
        return blockIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess iba, int x, int y, int z, int s) {
        return getIcon(s, iba.getBlockMetadata(x, y, z));
    }
}
