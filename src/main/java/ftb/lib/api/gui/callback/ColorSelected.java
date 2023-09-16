package ftb.lib.api.gui.callback;

import latmod.lib.LMColor;

public class ColorSelected {

    public final Object ID;
    public final boolean set;
    public final LMColor color;
    public final boolean closeGui;

    public ColorSelected(Object id, boolean s, LMColor c, boolean g) {
        ID = id;
        set = s;
        color = c;
        closeGui = g;
    }
}
