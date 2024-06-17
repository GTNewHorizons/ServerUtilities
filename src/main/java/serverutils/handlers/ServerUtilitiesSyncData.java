package serverutils.handlers;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ISyncData;
import serverutils.task.ShutdownTask;

public class ServerUtilitiesSyncData implements ISyncData {

    @Override
    public NBTTagCompound writeSyncData(EntityPlayerMP player, ForgePlayer forgePlayer) {
        NBTTagCompound nbt = new NBTTagCompound();

        if (ShutdownTask.shutdownTime > 0L) {
            nbt.setLong("ShutdownTime", ShutdownTask.shutdownTime - System.currentTimeMillis());
        }

        return nbt;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readSyncData(NBTTagCompound nbt) {
        ServerUtilitiesClientEventHandler.readSyncData(nbt);
    }
}
