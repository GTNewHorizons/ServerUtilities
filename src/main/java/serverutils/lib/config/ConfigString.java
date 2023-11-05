package serverutils.lib.config;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.lib.icon.Color4I;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;

public class ConfigString extends ConfigValue {

    public static final String ID = "string";
    public static final Color4I COLOR = Color4I.rgb(0xFFAA49);

    public static class SimpleString extends ConfigString {

        private final Supplier<String> get;
        private final Consumer<String> set;

        public SimpleString(Supplier<String> g, Consumer<String> s, @Nullable Pattern pattern) {
            super("", pattern);
            get = g;
            set = s;
        }

        @Override
        public String getString() {
            return get.get();
        }

        @Override
        public void setString(String v) {
            set.accept(v);
        }
    }

    private String value;
    private Pattern pattern;

    public ConfigString(String v) {
        this(v, null);
    }

    public ConfigString(String v, @Nullable Pattern p) {
        value = v;
        pattern = p;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getString() {
        return value;
    }

    public void setString(String v) {
        value = v;
    }

    @Nullable
    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(@Nullable Pattern p) {
        pattern = p;
    }

    @Override
    public boolean getBoolean() {
        return getString().equals("true");
    }

    @Override
    public int getInt() {
        return Integer.parseInt(getString());
    }

    @Override
    public double getDouble() {
        return Double.parseDouble(getString());
    }

    @Override
    public long getLong() {
        return Long.parseLong(getString());
    }

    @Override
    public ConfigString copy() {
        return new ConfigString(getString(), getPattern());
    }

    @Override
    public Color4I getColor() {
        return COLOR;
    }

    @Override
    public IChatComponent getStringForGUI() {
        return new ChatComponentText('"' + getString() + '"');
    }

    @Override
    public boolean setValueFromString(@Nullable ICommandSender sender, String string, boolean simulate) {
        if (string.length() >= 2 && string.charAt(0) == '"' && string.charAt(string.length() - 1) == '"') {
            return setValueFromString(sender, string.substring(1, string.length() - 1), simulate);
        }

        if (getPattern() != null && !getPattern().matcher(string).matches()) {
            return false;
        }

        if (!simulate) {
            setString(string);
        }

        return true;
    }

    @Override
    public void addInfo(ConfigValueInstance inst, List<String> list) {
        super.addInfo(inst, list);

        if (getPattern() != null) {
            list.add(EnumChatFormatting.AQUA + "Regex: " + EnumChatFormatting.RESET + getPattern().pattern());
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        value = getString();

        if (!value.isEmpty()) {
            nbt.setString(key, value);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        setString(nbt.getString(key));
    }

    @Override
    public void writeData(DataOut data) {
        data.writeString(getString());
        data.writeString(getPattern() == null ? "" : getPattern().pattern());
    }

    @Override
    public void readData(DataIn data) {
        setString(data.readString());
        String p = data.readString();
        setPattern(p.isEmpty() ? null : Pattern.compile(p));
    }

    @Override
    public boolean isEmpty() {
        return getString().isEmpty();
    }

    @Override
    public void setValueFromOtherValue(ConfigValue value) {
        setString(value.getString());
    }
}
