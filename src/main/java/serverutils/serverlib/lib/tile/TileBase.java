package serverutils.serverlib.lib.tile;

import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import serverutils.serverlib.lib.math.BlockDimPos;
import serverutils.serverlib.lib.util.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundEventAccessor; //SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IWorldAccess IWorldNameable;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TileBase extends TileEntity implements IWorldNameable, IChangeCallback
{
	public boolean brokenByCreative = false;
	private boolean isDirty = true;
	private IBlockState currentState;

	protected abstract void writeData(NBTTagCompound nbt, EnumSaveType type);

	protected abstract void readData(NBTTagCompound nbt, EnumSaveType type);

	@Override
	public String getName()
	{
		return getDisplayName().getFormattedText();
	}

	@Override
	public boolean hasCustomName()
	{
		return false;
	}

	@Override
	@Nonnull
	public IChatComponent getDisplayName()
	{
		return new ChatComponentTranslation(getBlockType().getUnlocalizedName() + ".name");
	}

	@Override
	public final NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		nbt = super.writeToNBT(nbt);
		writeData(nbt, EnumSaveType.SAVE);
		return nbt;
	}

	@Override
	public final void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		readData(nbt, EnumSaveType.SAVE);
	}

	@Override
	@Nullable
	public final SPacketUpdateTileEntity getUpdatePacket()
	{
		NBTTagCompound nbt = super.writeToNBT(new NBTTagCompound());
		writeData(nbt, EnumSaveType.NET_UPDATE);
		nbt.removeTag("id");
		nbt.removeTag("x");
		nbt.removeTag("y");
		nbt.removeTag("z");
		return nbt.isEmpty() ? null : new SPacketUpdateTileEntity(pos, 0, nbt);
	}

	@Override
	public final void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
	{
		readData(pkt.getNbtCompound(), EnumSaveType.NET_UPDATE);
		onUpdatePacket(EnumSaveType.NET_UPDATE);
	}

	@Override
	public final NBTTagCompound getUpdateTag()
	{
		NBTTagCompound nbt = super.getUpdateTag();
		writeData(nbt, EnumSaveType.NET_FULL);
		nbt.removeTag("id");
		return nbt;
	}

	@Override
	public final void handleUpdateTag(NBTTagCompound tag)
	{
		readData(tag, EnumSaveType.NET_FULL);
		onUpdatePacket(EnumSaveType.NET_FULL);
	}

	public void onUpdatePacket(EnumSaveType type)
	{
		markDirty();
	}

	protected boolean notifyBlock()
	{
		return true;
	}

	public boolean updateComparator()
	{
		return false;
	}

	@Override
	public void onLoad()
	{
		isDirty = true;
	}

	@Override
	public void markDirty()
	{
		isDirty = true;
	}

	@Override
	public void onContentsChanged(boolean majorChange)
	{
		markDirty();
	}

	public final void checkIfDirty()
	{
		if (isDirty)
		{
			sendDirtyUpdate();
			isDirty = false;
		}
	}

	@Override
	public final Block getBlockType()
	{
		return getBlockState().getBlock();
	}

	@Override
	public final int getBlockMetadata()
	{
		return getBlockState().getBlock().getMetaFromState(getBlockState());
	}

	protected void sendDirtyUpdate()
	{
		updateContainingBlockInfo();

		if (world != null)
		{
			world.markChunkDirty(pos, this);

			if (notifyBlock())
			{
				BlockUtils.notifyBlockUpdate(world, pos, getBlockState());
			}

			if (updateComparator() && getBlockType() != Blocks.AIR)
			{
				world.updateComparatorOutputLevel(pos, getBlockType());
			}
		}
	}

	@Override
	public void updateContainingBlockInfo()
	{
		super.updateContainingBlockInfo();
		currentState = null;
	}

	@Override
	public boolean shouldRefresh(World world, BlockDimPos pos, IBlockState oldState, IBlockState newSate)
	{
		updateContainingBlockInfo();
		return oldState.getBlock() != newSate.getBlock();
	}

	public IBlockState createState(IBlockState state)
	{
		return state;
	}

	public IBlockState getBlockState()
	{
		if (currentState == null)
		{
			if (world == null)
			{
				return BlockUtils.AIR_STATE;
			}

			currentState = createState(world.getBlockState(getPos()));
		}

		return currentState;
	}

	public void notifyNeighbors()
	{
		world.notifyNeighborsOfStateChange(getPos(), getBlockType(), false);
	}

	public void playSound(SoundEventAccessor event, SoundCategory category, float volume, float pitch)
	{
		world.playSound(null, getDimPos().posX + 0.5D, getDimPos().posY + 0.5D, getDimPos().posZ + 0.5D, event, category, volume, pitch);
	}

	public BlockDimPos getDimPos()
	{
		return new BlockDimPos(getDimPos(), hasWorld() ? worldObj.provider.getDimensionName() : 0);
	}

	public void writeToPickBlock(ItemStack stack)
	{
		writeToItem(stack);
	}

	public void writeToItem(ItemStack stack)
	{
		NBTTagCompound nbt = new NBTTagCompound();
		writeData(nbt, EnumSaveType.ITEM);

		if (!nbt.hasNoTags())
		{
			ResourceLocation id = getKey(getClass());

			if (id != null)
			{
				nbt.setString("id", id.toString());
			}

			NBTTagList lore = new NBTTagList();
			lore.appendTag(new NBTTagString("(+NBT)"));
			NBTTagCompound display = new NBTTagCompound();
			display.setTag("Lore", lore);
			stack.setTagInfo("display", display);
			stack.setTagInfo(BlockUtils.DATA_TAG, nbt);
		}
	}

	public void readFromItem(ItemStack stack)
	{
		NBTTagCompound nbt = BlockUtils.getData(stack);

		if (!nbt.hasNoTags())
		{
			readData(nbt, EnumSaveType.ITEM);
		}
	}
}