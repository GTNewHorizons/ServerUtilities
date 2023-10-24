package serverutils.lib.config;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;

import com.google.gson.JsonElement;

import serverutils.lib.gui.IOpenableGui;
import serverutils.lib.icon.Color4I;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.util.misc.MouseButton;

public class ConfigNull extends ConfigValue {

    public static final String ID = "null";
    public static final ConfigNull INSTANCE = new ConfigNull();
    public static final Color4I COLOR = Color4I.rgb(0x333333);

    private ConfigNull() {}

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getString() {
        return "null";
    }

    @Override
    public boolean getBoolean() {
        return false;
    }

    @Override
    public int getInt() {
        return 0;
    }

    @Override
    public ConfigNull copy() {
        return this;
    }

    @Override
    public boolean equalsValue(ConfigValue value) {
        return value == this;
    }

    @Override
    public Color4I getColor() {
        return COLOR;
    }

    @Override
    public void addInfo(ConfigValueInstance inst, List<String> list) {}

    @Override
    public void onClicked(IOpenableGui gui, ConfigValueInstance inst, MouseButton button, Runnable callback) {}

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {}

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {}

    @Override
    public void writeData(DataOut data) {}

    @Override
    public void readData(DataIn data) {}

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate) {
        return false;
    }

    @Override
    public void setValueFromOtherValue(ConfigValue value) {}

    @Override
    public void setValueFromJson(JsonElement json) {}
}
