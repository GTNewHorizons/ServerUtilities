package serverutils.lib.config;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;

import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.MutableColor4I;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;

public class ConfigColor extends ConfigValue {

    public static final String ID = "color";

    private final MutableColor4I value = Color4I.WHITE.mutable();

    public ConfigColor(Color4I v) {
        value.set(v, 255);
    }

    public ConfigColor(int col) {
        value.set(0xFF000000 | col);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public MutableColor4I getColor() {
        return value;
    }

    @Override
    public String getString() {
        return getColor().toString();
    }

    @Override
    public boolean getBoolean() {
        return !getColor().isEmpty();
    }

    @Override
    public int getInt() {
        return getColor().rgba();
    }

    @Override
    public ConfigColor copy() {
        return new ConfigColor(getColor());
    }

    @Override
    public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate) {
        try {
            if (string.indexOf(',') != -1) {
                if (string.length() < 5) {
                    return false;
                }

                String[] s = string.split(",");

                if (s.length == 3 || s.length == 4) {
                    int[] c = new int[4];
                    c[3] = 255;

                    for (int i = 0; i < s.length; i++) {
                        c[i] = Integer.parseInt(s[i]);
                    }

                    if (!simulate) {
                        getColor().set(c[0], c[1], c[2], c[3]);
                    }

                    return true;
                }
            } else {
                if (string.length() < 6) {
                    return false;
                } else if (string.startsWith("#")) {
                    string = string.substring(1);
                }

                int hex = Integer.parseInt(string, 16);

                if (!simulate) {
                    getColor().set(0xFF000000 | hex);
                }

                return true;
            }
        } catch (Exception ex) {}

        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        nbt.setInteger(key, getInt());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        getColor().set(0xFF000000 | nbt.getInteger(key));
    }

    @Override
    public void writeData(DataOut data) {
        data.writeInt(getInt());
    }

    @Override
    public void readData(DataIn data) {
        getColor().set(data.readInt());
    }

    @Override
    public void setValueFromOtherValue(ConfigValue value) {
        getColor().set(value.getColor());
    }
}
