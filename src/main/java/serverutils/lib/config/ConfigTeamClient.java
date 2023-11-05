package serverutils.lib.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTBase.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.IChatComponent;

import serverutils.lib.data.ForgeTeam;
import serverutils.lib.gui.IOpenableGui;
import serverutils.lib.gui.misc.GuiSelectTeamValue;
import serverutils.lib.icon.Color4I;
import serverutils.lib.icon.Icon;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.util.FinalIDObject;
import serverutils.lib.util.misc.MouseButton;

public class ConfigTeamClient extends ConfigString {

    public static class TeamInst extends FinalIDObject {

        public final short uid;
        public IChatComponent title;
        public Icon icon;

        public TeamInst(short u, String id) {
            super(id);
            uid = u;
        }
    }

    public final Map<String, TeamInst> map;

    public ConfigTeamClient(String v) {
        super(v, ForgeTeam.TEAM_ID_PATTERN);
        map = new HashMap<>();
    }

    @Override
    public String getId() {
        return ConfigTeam.TEAM_ID;
    }

    @Override
    public IChatComponent getStringForGUI() {
        TeamInst inst = map.get(getString());

        if (inst != null) {
            return inst.title.createCopy();
        }

        return super.getStringForGUI();
    }

    @Override
    public ConfigTeamClient copy() {
        ConfigTeamClient config = new ConfigTeamClient(getString());

        for (TeamInst inst : map.values()) {
            TeamInst inst1 = new TeamInst(inst.uid, inst.getId());
            inst1.title = inst.title.createCopy();
            inst1.icon = inst.icon.copy();
            config.map.put(inst1.getId(), inst1);
        }

        return config;
    }

    @Override
    public Color4I getColor() {
        return Color4I.LIGHT_GREEN;
    }

    @Override
    public void addInfo(ConfigValueInstance inst, List<String> list) {}

    @Override
    public List<String> getVariants() {
        return new ArrayList<>(map.keySet());
    }

    @Override
    public void onClicked(IOpenableGui gui, ConfigValueInstance inst, MouseButton button, Runnable callback) {
        if (inst.getCanEdit()) {
            new GuiSelectTeamValue(this, gui, callback).openGui();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String key) {
        nbt.setString(key, getString());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String key) {
        NBTBase id = nbt.getTag(key);

        if (id instanceof NBTTagString tagString) {
            setString(tagString.func_150285_a_());
        } else if (id instanceof NBTPrimitive tagPrimitive) {
            ForgeTeam team = null;
            short ids = tagPrimitive.func_150289_e();

            for (TeamInst inst : map.values()) {
                if (inst.uid == ids) {
                    setString(inst.getId());
                    return;
                }
            }
        }
    }

    @Override
    public void writeData(DataOut data) {
        throw new IllegalStateException("Can't write Team client property!");
    }

    @Override
    public void readData(DataIn data) {
        int s = data.readVarInt();
        map.clear();

        for (int i = 0; i < s; i++) {
            short uid = data.readShort();
            String id = data.readString();
            TeamInst inst = new TeamInst(uid, id);
            inst.title = data.readTextComponent();
            inst.icon = data.readIcon();
            map.put(inst.getId(), inst);
        }

        setString(data.readString());
    }
}
