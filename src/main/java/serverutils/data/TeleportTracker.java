package serverutils.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;

import com.mojang.authlib.GameProfile;

import serverutils.lib.math.BlockDimPos;
import serverutils.lib.util.INBTSerializable;
import serverutils.lib.util.permission.IPermissionHandler;
import serverutils.lib.util.permission.PermissionAPI;

public class TeleportTracker implements INBTSerializable<NBTTagCompound> {

    private final TeleportLog[] logs;
    private final IPermissionHandler permissionHandler;

    public TeleportTracker() {
        this(PermissionAPI.getPermissionHandler());
    }

    public TeleportTracker(IPermissionHandler permissionHandler) {
        this.logs = new TeleportLog[TeleportType.values().length];
        this.permissionHandler = permissionHandler;
    }

    public void logTeleport(TeleportType teleportType, BlockDimPos from, long worldTime) {
        logs[teleportType.ordinal()] = new TeleportLog(teleportType, from, worldTime);
    }

    public TeleportLog getLastDeath() {
        return logs[TeleportType.RESPAWN.ordinal()];
    }

    private TeleportLog[] getSortedLogs() {
        TeleportLog[] toSort = Arrays.stream(logs).filter(Objects::nonNull).toArray(TeleportLog[]::new);
        Arrays.sort(toSort, Collections.reverseOrder());
        return toSort;
    }

    // Returns latest available according to permissions.
    public TeleportLog getLastAvailableLog(GameProfile gameProfile) {
        for (TeleportLog l : getSortedLogs()) {
            if (permissionHandler.hasPermission(gameProfile, l.teleportType.getPermission(), null)) {
                return l;
            }
        }
        return null;
    }

    public TeleportLog getLastLog() {
        TeleportLog[] logs = getSortedLogs();
        return logs[0];
    }

    public long getLastTeleportTime(TeleportType teleportType) {
        TeleportLog log = logs[teleportType.ordinal()];
        return log == null ? -1 : log.getCreatedAt();
    }

    public void clearLog(TeleportType teleportType) {
        logs[teleportType.ordinal()] = null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        for (int i = 0; i < logs.length; i++) {
            if (logs[i] == null) continue;
            nbt.setTag(String.valueOf(i), logs[i].serializeNBT());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        for (int i = 0; i < logs.length; i++) {
            String key = String.valueOf(i);
            if (nbt.hasKey(key)) {
                logs[i] = new TeleportLog(nbt.getCompoundTag(key));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (int i = 0; i < logs.length; i++) {
            final TeleportLog l = logs[i];
            builder.append(l.teleportType.toString()).append(":").append(l.getBlockDimPos());
            if (i != logs.length - 1) {
                builder.append(",");
            }
        }
        builder.append("}");
        return builder.toString();
    }
}
