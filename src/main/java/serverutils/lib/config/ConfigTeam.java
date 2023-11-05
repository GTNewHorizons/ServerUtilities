package serverutils.lib.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTBase.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.IChatComponent;

import serverutils.lib.data.ForgeTeam;
import serverutils.lib.gui.IOpenableGui;
import serverutils.lib.icon.Color4I;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.util.misc.MouseButton;

public class ConfigTeam extends ConfigValue {

    public static final String TEAM_ID = "team";

    private final Supplier<ForgeTeam> get;
    private final Consumer<ForgeTeam> set;

    public ConfigTeam(Supplier<ForgeTeam> g, Consumer<ForgeTeam> s) {
        get = g;
        set = s;
    }

    @Override
    public String getId() {
        return TEAM_ID;
    }

    @Override
    public IChatComponent getStringForGUI() {
        return get.get().getTitle();
    }

    @Override
    public String getString() {
        return get.get().getId();
    }

    @Override
    public boolean getBoolean() {
        return get.get().isValid();
    }

    @Override
    public int getInt() {
        return get.get().getUID();
    }

    @Override
    public ConfigTeam copy() {
        throw new IllegalStateException("Not supported!");
    }

    @Override
    public Color4I getColor() {
        return get.get().getColor().getColor();
    }

    @Override
    public void addInfo(ConfigValueInstance inst, List<String> list) {}

    @Override
    public List<String> getVariants() {
        List<String> list = new ArrayList<>();

        for (ForgeTeam team : get.get().universe.getTeams()) {
            list.add(team.getId());
        }

        list.sort(null);
        return list;
    }

    @Override
    public void onClicked(IOpenableGui gui, ConfigValueInstance inst, MouseButton button, Runnable callback) {}

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        nbt.setShort(key, (short) getInt());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        NBTBase id = nbt.getTag(key);

        if (id instanceof NBTTagString tagString) {
            set.accept(get.get().universe.getTeam(tagString.func_150285_a_()));
        } else if (id instanceof NBTPrimitive tagPrimitive) {
            set.accept(get.get().universe.getTeam(tagPrimitive.func_150289_e()));
        }
    }

    @Override
    public void writeData(DataOut data) {
        ForgeTeam team = get.get();
        Collection<ForgeTeam> teams = team.universe.getTeams();
        data.writeVarInt(teams.size());

        for (ForgeTeam t : teams) {
            data.writeShort(t.getUID());
            data.writeString(t.getId());
            data.writeTextComponent(t.getTitle());
            data.writeIcon(t.getIcon());
        }

        data.writeString(getString());
    }

    @Override
    public void readData(DataIn data) {
        throw new IllegalStateException("Can't read Team property!");
    }
}
