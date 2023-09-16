package ftb.lib.mod;

import java.util.UUID;

import net.minecraft.entity.player.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import ftb.lib.api.tile.IGuiTile;

public class FTBLibModCommon // FTBLibModClient
{

    public void preInit() {}

    public void postInit() {}

    public String translate(String key, Object... obj) {
        if (obj == null || obj.length == 0) return StatCollector.translateToLocal(key);
        return StatCollector.translateToLocalFormatted(key, obj);
    }

    public boolean isShiftDown() {
        return false;
    }

    public boolean isCtrlDown() {
        return false;
    }

    public boolean isTabDown() {
        return false;
    }

    public boolean inGameHasFocus() {
        return false;
    }

    public EntityPlayer getClientPlayer() {
        return null;
    }

    public EntityPlayer getClientPlayer(UUID id) {
        return null;
    }

    public World getClientWorld() {
        return null;
    }

    public double getReachDist(EntityPlayer ep) {
        if (ep instanceof EntityPlayerMP) return ((EntityPlayerMP) ep).theItemInWorldManager.getBlockReachDistance();
        return 0D;
    }

    public void spawnDust(World worldObj, double x, double y, double z, int i) {}

    public boolean openClientGui(EntityPlayer ep, String mod, int ID, NBTTagCompound data) {
        return false;
    }

    public void openClientTileGui(EntityPlayer ep, IGuiTile t, NBTTagCompound data) {}
}
