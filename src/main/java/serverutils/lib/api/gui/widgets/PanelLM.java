package serverutils.lib.api.gui.widgets;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.api.gui.IGuiLM;

@SideOnly(Side.CLIENT)
public abstract class PanelLM extends WidgetLM // GuiLM
{

    public final List<WidgetLM> widgets;
    protected final List<PanelLM> childPanels;

    public PanelLM(IGuiLM g, int x, int y, int w, int h) {
        super(g, x, y, w, h);
        widgets = new ArrayList<>();
        childPanels = new ArrayList<>();
    }

    public abstract void addWidgets();

    public void add(WidgetLM w) {
        if (w == null) return;
        w.parentPanel = this;
        widgets.add(w);

        if (w instanceof PanelLM) {
            PanelLM p = (PanelLM) w;
            childPanels.add(p);
            p.refreshWidgets();
        }
    }

    public void addAll(WidgetLM[] l) {
        if (l == null || l.length == 0) return;
        for (int i = 0; i < l.length; i++) add(l[i]);
    }

    public void addAll(List<? extends WidgetLM> l) {
        if (l == null || l.isEmpty()) return;
        for (WidgetLM w : l) add(w);
    }

    public void refreshWidgets() {
        widgets.clear();
        addWidgets();
    }

    public void addMouseOverText(List<String> l) {
        if (title != null) l.add(title);
        for (WidgetLM w : widgets) if (w.isEnabled() && w.mouseOver()) w.addMouseOverText(l);
    }

    public void mousePressed(int b) {
        for (WidgetLM w : widgets) if (w.isEnabled()) w.mousePressed(b);
    }

    public boolean keyPressed(int key, char keyChar) {
        for (WidgetLM w : widgets) if (w.isEnabled() && w.keyPressed(key, keyChar)) return true;
        return false;
    }

    public void renderWidget() {
        for (int i = 0; i < widgets.size(); i++) widgets.get(i).renderWidget();
    }
}
