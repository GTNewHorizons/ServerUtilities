package serverutils.lib.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import serverutils.lib.util.BlockUtils;

// TODO: FIX
public abstract class TileBase extends TileEntity implements IChangeCallback {

    public boolean brokenByCreative = false;
    private boolean isDirty = true;

    protected abstract void writeData(NBTTagCompound nbt, EnumSaveType type);

    protected abstract void readData(NBTTagCompound nbt, EnumSaveType type);

    // public String getName() {
    // return getDisplayName().getFormattedText();
    // }

    public boolean hasCustomName() {
        return false;
    }

    public IChatComponent getDisplayName() {
        return new ChatComponentTranslation(getBlockType().getUnlocalizedName() + ".name");
    }

    @Override
    public final void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        writeData(nbt, EnumSaveType.SAVE);
    }

    @Override
    public final void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        readData(nbt, EnumSaveType.SAVE);
    }

    public final S35PacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        super.writeToNBT(nbt);
        writeData(nbt, EnumSaveType.NET_UPDATE);
        nbt.removeTag("id");
        nbt.removeTag("x");
        nbt.removeTag("y");
        nbt.removeTag("z");
        return nbt.hasNoTags() ? null : new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
    }

    @Override
    public final void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readData(pkt.func_148857_g(), EnumSaveType.NET_UPDATE);
        onUpdatePacket(EnumSaveType.NET_UPDATE);
    }

    public final NBTTagCompound getUpdateTag() {
        NBTTagCompound nbt = new NBTTagCompound();
        super.writeToNBT(nbt);
        writeData(nbt, EnumSaveType.NET_FULL);
        nbt.removeTag("id");
        return nbt;
    }

    public final void handleUpdateTag(NBTTagCompound tag) {
        readData(tag, EnumSaveType.NET_FULL);
        onUpdatePacket(EnumSaveType.NET_FULL);
    }

    public void onUpdatePacket(EnumSaveType type) {
        markDirty();
    }

    protected boolean notifyBlock() {
        return true;
    }

    public boolean updateComparator() {
        return false;
    }

    public void onLoad() {
        isDirty = true;
    }

    @Override
    public void markDirty() {
        isDirty = true;
    }

    @Override
    public void onContentsChanged(boolean majorChange) {
        markDirty();
    }

    // public final void checkIfDirty() {
    // if (isDirty) {
    // sendDirtyUpdate();
    // isDirty = false;
    // }
    // }

    // @Override
    // public final Block getBlockType() {
    // return getBlockState().getBlock();
    // }

    // @Override
    // public final int getBlockMetadata() {
    // return getBlockState().getBlock().getMetaFromState(getBlockState());
    // }

    // protected void sendDirtyUpdate() {
    // updateContainingBlockInfo();

    // if (worldObj != null) {
    // worldObj.markChunkDirty(pos, this);

    // if (notifyBlock()) {
    // BlockUtils.notifyBlockUpdate(worldObj, xCoord, yCoord, zCoord);
    // }

    // if (updateComparator() && getBlockType() != null) {
    // worldObj.updateComparatorOutputLevel(xCoord, yCoord, zCoord, getBlockType());
    // }
    // }
    // }

    // @Override
    // public void updateContainingBlockInfo() {
    // super.updateContainingBlockInfo();
    // currentState = null;
    // }

    // @Override
    // public boolean shouldRefresh(World world, int posx, int posy, int posz, IBlockState oldState, IBlockState
    // newSate) {
    // updateContainingBlockInfo();
    // return oldState.getBlock() != newSate.getBlock();
    // }

    // public IBlockState createState(IBlockState state) {
    // return state;
    // }

    // public IBlockState getBlockState() {
    // if (currentState == null) {
    // if (world == null) {
    // return BlockUtils.AIR_STATE;
    // }

    // currentState = createState(world.getBlockState(getPos()));
    // }

    // return currentState;
    // }

    // public void notifyNeighbors() {
    // world.notifyNeighborsOfStateChange(getPos(), getBlockType(), false);
    // }

    // public void playSound(SoundEvent event, SoundCategory category, float volume, float pitch) {
    // world.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, event, category, volume, pitch);
    // }

    // public BlockDimPos getDimPos() {
    // return new BlockDimPos(pos, hasWorld() ? world.provider.getDimension() : 0);
    // }

    // public void writeToPickBlock(ItemStack stack) {
    // writeToItem(stack);
    // }

    // public void writeToItem(ItemStack stack) {
    // NBTTagCompound nbt = new NBTTagCompound();
    // writeData(nbt, EnumSaveType.ITEM);

    // if (!nbt.hasNoTags()) {
    // TileEntity.addMapping(getClass(), getName());
    // ResourceLocation id = getKey(getClass());

    // if (id != null) {
    // nbt.setString("id", id.toString());
    // }

    // NBTTagList lore = new NBTTagList();
    // lore.appendTag(new NBTTagString("(+NBT)"));
    // NBTTagCompound display = new NBTTagCompound();
    // display.setTag("Lore", lore);
    // stack.setTagInfo("display", display);
    // stack.setTagInfo(BlockUtils.DATA_TAG, nbt);
    // }
    // }

    public void readFromItem(ItemStack stack) {
        NBTTagCompound nbt = BlockUtils.getData(stack);

        if (!nbt.hasNoTags()) {
            readData(nbt, EnumSaveType.ITEM);
        }
    }
}
