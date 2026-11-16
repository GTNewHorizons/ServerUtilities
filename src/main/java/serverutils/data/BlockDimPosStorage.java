package serverutils.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;

import serverutils.lib.math.BlockDimPos;
import serverutils.lib.math.TeleporterDimPos;
import serverutils.lib.util.INBTSerializable;
import serverutils.lib.util.StringUtils;

public final class BlockDimPosStorage implements INBTSerializable<NBTTagCompound> {

    private final Map<String, StoredPosition> map = new HashMap<>();
    private final List<String> names = new ArrayList<>();

    public Collection<String> list() {
        return names;
    }

    @Nullable
    public BlockDimPos get(String s) {
        StoredPosition position = map.get(s);
        return position == null ? null : position.pos;
    }

    @Nullable
    public TeleporterDimPos getTeleporter(String name, boolean restoreRotation) {
        StoredPosition position = map.get(name);
        if (position == null) return null;
        TeleporterDimPos teleporter = position.pos.teleporter();
        return restoreRotation && position.yaw != null ? teleporter.withRotation(position.yaw, position.pitch)
                : teleporter;
    }

    public boolean set(String name, @Nullable BlockDimPos pos) {
        return setPosition(name, pos == null ? null : new StoredPosition(pos.copy(), null, null));
    }

    public boolean set(String name, BlockDimPos pos, float yaw, float pitch) {
        return setPosition(name, new StoredPosition(pos.copy(), yaw, pitch));
    }

    private boolean setPosition(String name, @Nullable StoredPosition position) {
        if (position == null) {
            if (map.remove(name) != null) {
                names.remove(name);
                return true;
            }

            return false;
        }

        if (map.put(name, position) == null) {
            names.add(name);
            names.sort(StringUtils.IGNORE_CASE_COMPARATOR);
        }

        return false;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int size() {
        return map.size();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();

        for (Map.Entry<String, StoredPosition> entry : map.entrySet()) {
            StoredPosition position = entry.getValue();
            int[] values = position.pos.toIntArray();
            if (position.yaw != null) {
                // Keep the legacy coordinate prefix; older readers ignore the optional yaw/pitch float bits.
                values = Arrays.copyOf(values, 6);
                values[4] = Float.floatToIntBits(position.yaw);
                values[5] = Float.floatToIntBits(position.pitch);
            }
            nbt.setIntArray(entry.getKey().toLowerCase(), values);
        }

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        map.clear();
        names.clear();

        for (String name : nbt.func_150296_c()) {
            int[] values = nbt.getIntArray(name);
            BlockDimPos pos = BlockDimPos.fromIntArray(values);

            if (pos != null) {
                Float yaw = values.length >= 6 ? Float.intBitsToFloat(values[4]) : null;
                Float pitch = values.length >= 6 ? Float.intBitsToFloat(values[5]) : null;
                map.put(name.toLowerCase(), new StoredPosition(pos, yaw, pitch));
            }
        }

        names.addAll(map.keySet());
        names.sort(StringUtils.IGNORE_CASE_COMPARATOR);
    }

    private static final class StoredPosition {

        private final BlockDimPos pos;
        private final Float yaw, pitch;

        private StoredPosition(BlockDimPos pos, Float yaw, Float pitch) {
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
