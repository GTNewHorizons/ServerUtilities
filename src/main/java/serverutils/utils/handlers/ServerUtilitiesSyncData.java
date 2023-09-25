package serverutils.utils.handlers;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.ISyncData;
import serverutils.utils.data.ServerUtilitiesUniverseData;

public class ServerUtilitiesSyncData implements ISyncData {

    @Override
    public NBTTagCompound writeSyncData(EntityPlayerMP player, ForgePlayer forgePlayer) {
        NBTTagCompound nbt = new NBTTagCompound();

        if (ServerUtilitiesUniverseData.shutdownTime > 0L) {
            nbt.setLong("ShutdownTime", ServerUtilitiesUniverseData.shutdownTime - System.currentTimeMillis());
        }

        return nbt;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readSyncData(NBTTagCompound nbt) {
        ServerUtilitiesClientEventHandler.readSyncData(nbt);
    }
}
