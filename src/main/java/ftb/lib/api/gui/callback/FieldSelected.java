package ftb.lib.api.gui.callback;

public class FieldSelected {

    public final Object ID;
    public final boolean set;
    public final String result;
    public final boolean closeGui;

    public FieldSelected(Object id, boolean s, String r, boolean g) {
        ID = id;
        set = s;
        result = r;
        closeGui = g;
    }

    public int resultI() {
        return Integer.parseInt(result);
    }

    public double resultD() {
        return Double.parseDouble(result);
    }
}
