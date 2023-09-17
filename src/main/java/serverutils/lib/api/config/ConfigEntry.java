package serverutils.lib.api.config;

import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.IJsonSerializable;
import net.minecraftforge.common.util.Constants;

import com.google.gson.JsonElement;

import latmod.lib.Bits;
import latmod.lib.IntList;
import latmod.lib.annotations.IFlagContainer;
import latmod.lib.annotations.IInfoContainer;
import latmod.lib.util.FinalIDObject;

public abstract class ConfigEntry extends FinalIDObject implements IInfoContainer, IFlagContainer, IJsonSerializable {

    public ConfigGroup parentGroup;
    private String[] info;
    protected byte flags = 0;

    ConfigEntry(String id) {
        super(id);
    }

    public abstract ConfigEntryType getConfigType();

    public abstract void func_152753_a(JsonElement o);

    public abstract JsonElement getSerializableElement();

    public int getColor() {
        return 0x999999;
    }

    public String getFullID() {
        if (parentGroup == null) return getID();
        return parentGroup.getFullID() + '.' + getID();
    }

    public String getDefValueString() {
        return null;
    }

    public String getMinValueString() {
        return null;
    }

    public String getMaxValueString() {
        return null;
    }

    public ConfigEntry copy() {
        ConfigEntry e = getConfigType().createNew(getID());
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag, true);
        e.readFromNBT(tag, true);
        return e;
    }

    public final String toString() {
        return getAsString();
    }

    public abstract String getAsString();

    public boolean getAsBoolean() {
        return false;
    }

    public int getAsInt() {
        return 0;
    }

    public double getAsDouble() {
        return 0D;
    }

    public IntList getAsIntList() {
        return new IntList(new int[] { getAsInt() });
    }

    public List<String> getAsStringList() {
        return Collections.singletonList(getAsString());
    }

    public ConfigGroup getAsGroup() {
        return null;
    }

    public final void setFlag(byte flag, boolean b) {
        flags = Bits.setBit(flags, flag, b);
    }

    public final boolean getFlag(byte flag) {
        return Bits.getBit(flags, flag);
    }

    public final void setInfo(String[] s) {
        info = (s != null && s.length > 0) ? s : null;
    }

    public final String[] getInfo() {
        return info;
    }

    public void writeToNBT(NBTTagCompound tag, boolean extended) {
        if (extended) {
            if (flags != 0) tag.setByte("F", flags);

            if (info != null && info.length > 0) {
                NBTTagList list = new NBTTagList();

                for (int i = 0; i < info.length; i++) {
                    list.appendTag(new NBTTagString(info[i]));
                }

                tag.setTag("I", list);
            }
        }
    }

    public void readFromNBT(NBTTagCompound tag, boolean extended) {
        if (extended) {
            flags = tag.getByte("F");
            info = null;

            if (tag.hasKey("I")) {
                NBTTagList list = tag.getTagList("I", Constants.NBT.TAG_STRING);

                info = new String[list.tagCount()];

                for (int i = 0; i < info.length; i++) {
                    info[i] = list.getStringTagAt(i);
                }
            }
        }
    }
}
