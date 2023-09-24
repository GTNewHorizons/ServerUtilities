package serverutils.serverlib.lib.data;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author LatvianModder
 */
public interface ISyncData
{
	NBTTagCompound writeSyncData(EntityPlayerMP player, ForgePlayer forgePlayer);

	@SideOnly(Side.CLIENT)
	void readSyncData(NBTTagCompound nbt);
}