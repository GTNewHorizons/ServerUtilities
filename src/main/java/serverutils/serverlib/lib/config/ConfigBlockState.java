package serverutils.serverlib.lib.config;

// import serverutils.serverlib.io.lib.DataIn;
// import serverutils.serverlib.io.lib.DataOut;
// import serverutils.serverlib.util.lib.BlockUtils;
// import com.google.gson.JsonElement;
// import net.minecraft.block.Block;
// import net.minecraft.command.ICommandSender;
// import net.minecraft.nbt.NBTTagCompound;
//
// import javax.annotation.Nullable;

//TODO: This
public class ConfigBlockState // extends ConfigValue
{
	// public static final String ID = "blockstate";
	//
	// private IBlockState value;
	//
	// public ConfigBlockState(IBlockState state)
	// {
	// value = state;
	// }
	//
	// @Override
	// public String getId()
	// {
	// return ID;
	// }
	//
	// public IBlockState getBlockState()
	// {
	// return value;
	// }
	//
	// public void setBlockState(IBlockState state)
	// {
	// value = state;
	// }
	//
	// @Override
	// public String getString()
	// {
	// return BlockUtils.getNameFromState(getBlockState());
	// }
	//
	// @Override
	// public boolean getBoolean()
	// {
	// return getBlockState() != BlockUtils.AIR_STATE;
	// }
	//
	// @Override
	// public int getInt()
	// {
	// return Block.getStateId(getBlockState());
	// }
	//
	// @Override
	// public ConfigBlockState copy()
	// {
	// return new ConfigBlockState(getBlockState());
	// }
	//
	// @Override
	// public void writeToNBT(NBTTagCompound nbt, String key)
	// {
	// value = getBlockState();
	// nbt.setInteger(key, value == BlockUtils.AIR_STATE ? 0 : Block.getStateId(value));
	// }
	//
	// @Override
	// public void readFromNBT(NBTTagCompound nbt, String key)
	// {
	// int id = nbt.getInteger(key);
	// setBlockState(id == 0 ? BlockUtils.AIR_STATE : Block.getStateById(id));
	// }
	//
	// @Override
	// public void writeData(DataOut data)
	// {
	// data.writeBlockState(getBlockState());
	// }
	//
	// @Override
	// public void readData(DataIn data)
	// {
	// setBlockState(data.readBlockState());
	// }
	//
	// @Override
	// public boolean isEmpty()
	// {
	// return getBlockState() == BlockUtils.AIR_STATE;
	// }
	//
	// @Override
	// public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate)
	// {
	// if (!simulate)
	// {
	// setBlockState(BlockUtils.getStateFromName(string));
	// }
	//
	// return true;
	// }
	//
	// @Override
	// public void setValueFromOtherValue(ConfigValue value)
	// {
	// if (value instanceof ConfigBlockState)
	// {
	// setBlockState(((ConfigBlockState) value).getBlockState());
	// }
	// else
	// {
	// super.setValueFromOtherValue(value);
	// }
	// }
	//
	// @Override
	// public void setValueFromJson(JsonElement json)
	// {
	// if (json.isJsonPrimitive())
	// {
	// setBlockState(BlockUtils.getStateFromName(json.getAsString()));
	// }
	// }
}
