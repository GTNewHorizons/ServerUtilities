package serverutils.utils.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;

import serverutils.lib.lib.math.BlockDimPos;
import serverutils.lib.lib.util.INBTSerializable;
import serverutils.lib.lib.util.StringUtils;

public final class BlockDimPosStorage implements INBTSerializable<NBTTagCompound> {

    private final Map<String, BlockDimPos> map = new HashMap<>();
    private final List<String> names = new ArrayList<>();

    public Collection<String> list() {
        return names;
    }

    @Nullable
    public BlockDimPos get(String s) {
        return map.get(s);
    }

    public boolean set(String name, @Nullable BlockDimPos pos) {
        if (pos == null) {
            if (map.remove(name) != null) {
                names.remove(name);
                return true;
            }

            return false;
        }

        if (map.put(name, pos.copy()) == null) {
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

        for (Map.Entry<String, BlockDimPos> entry : map.entrySet()) {
            nbt.setIntArray(entry.getKey(), entry.getValue().toIntArray());
        }

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        map.clear();
        names.clear();

        for (String name : (Set<String>) nbt.func_150296_c()) {
            BlockDimPos pos = BlockDimPos.fromIntArray(nbt.getIntArray(name));

            if (pos != null) {
                map.put(name, pos);
            }
        }

        names.addAll(map.keySet());
        names.sort(StringUtils.IGNORE_CASE_COMPARATOR);
    }
}
