package ftb.lib.api.gui;

import ftb.lib.api.client.FTBLibClient;
import ftb.lib.api.gui.callback.IColorCallback;
import ftb.lib.api.gui.callback.IFieldCallback;
import ftb.lib.mod.client.gui.GuiSelectColor;
import ftb.lib.mod.client.gui.GuiSelectField;
import latmod.lib.Converter;
import latmod.lib.LMColor;

public class LMGuis {

    public static void displayColorSelector(IColorCallback cb, LMColor col, Object id, boolean instant) {
        FTBLibClient.openGui(new GuiSelectColor(cb, col, id, instant));
    }

    public static void displayFieldSelector(Object id, FieldType typ, Object d, IFieldCallback c) {
        FTBLibClient.openGui(new GuiSelectField(id, typ, String.valueOf(d), c));
    }

    public enum FieldType {

        STRING {

            public boolean isValid(String s) {
                return true;
            }
        },
        INTEGER {

            public boolean isValid(String s) {
                return Converter.canParseInt(s);
            }
        },
        DOUBLE {

            public boolean isValid(String s) {
                return Converter.canParseDouble(s);
            }
        };

        public abstract boolean isValid(String s);
    }
}
