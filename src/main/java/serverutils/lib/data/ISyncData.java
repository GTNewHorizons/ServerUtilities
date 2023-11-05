package serverutils.lib.data;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface ISyncData {

    NBTTagCompound writeSyncData(EntityPlayerMP player, ForgePlayer forgePlayer);

    @SideOnly(Side.CLIENT)
    void readSyncData(NBTTagCompound nbt);
}
