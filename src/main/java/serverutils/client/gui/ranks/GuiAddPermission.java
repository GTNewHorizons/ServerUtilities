package serverutils.client.gui.ranks;

import java.util.Arrays;
import java.util.List;

import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;

import serverutils.lib.config.ConfigGroup;
import serverutils.lib.config.ConfigValueInstance;
import serverutils.lib.gui.CheckBoxList;
import serverutils.lib.gui.Panel;
import serverutils.lib.gui.misc.GuiButtonListBase;
import serverutils.lib.util.misc.MouseButton;

public class GuiAddPermission extends GuiButtonListBase {

    private final ConfigGroup group;
    private final ConfigGroup allPerm;

    public GuiAddPermission(ConfigGroup rankPerms, ConfigGroup allPerm) {
        this.group = rankPerms;
        this.allPerm = allPerm;
        setHasSearchBox(true);
    }

    @Override
    public void addButtons(Panel panel) {
        panel.add(new PermissionList(panel, group));
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        boolean b = super.mousePressed(button);

        if (!b && !isMouseOver()) {
            parent.closeContextMenu();
            return true;
        }

        return b;
    }

    public class PermissionList extends CheckBoxList {

        public PermissionList(Panel parent, ConfigGroup rank) {
            super(parent, false);
            for (ConfigValueInstance inst : allPerm.getValues()) {
                CheckBoxList.CheckBoxEntry entry = new PermissionsEntry(inst);
                if (rank.getValueInstance(inst.getId()) != null) {
                    entry.value = 1;
                }
                addBox(entry);
            }
        }
    }

    public class PermissionsEntry extends CheckBoxList.CheckBoxEntry {

        private final ConfigValueInstance inst;

        public PermissionsEntry(ConfigValueInstance valueInstance) {
            super(valueInstance.getId());
            inst = valueInstance;
        }

        @Override
        public void addMouseOverText(List<String> list) {
            IChatComponent infoText = inst.getInfo();
            if (StatCollector.canTranslate("permission." + name)) {
                list.add(StatCollector.translateToLocal("permission." + name));
            } else if (!(infoText instanceof ChatComponentTranslation component)
                    || StatCollector.canTranslate(component.getKey())) {
                        list.addAll(Arrays.asList(infoText.getFormattedText().split("\n")));
                    }
        }

        @Override
        public void onValueChanged() {
            GuiEditRank p = (GuiEditRank) parent;
            if (value > 0) {
                p.addEntry(name);
            } else {
                p.removeEntry(name);
            }
        }
    }
}
