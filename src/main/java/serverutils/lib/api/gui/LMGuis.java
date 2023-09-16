package serverutils.lib.api.gui;

import latmod.lib.Converter;
import latmod.lib.LMColor;
import serverutils.lib.api.client.ServerUtilitiesLibraryClient;
import serverutils.lib.api.gui.callback.IColorCallback;
import serverutils.lib.api.gui.callback.IFieldCallback;
import serverutils.lib.mod.client.gui.GuiSelectColor;
import serverutils.lib.mod.client.gui.GuiSelectField;

public class LMGuis {

    public static void displayColorSelector(IColorCallback cb, LMColor col, Object id, boolean instant) {
        ServerUtilitiesLibraryClient.openGui(new GuiSelectColor(cb, col, id, instant));
    }

    public static void displayFieldSelector(Object id, FieldType typ, Object d, IFieldCallback c) {
        ServerUtilitiesLibraryClient.openGui(new GuiSelectField(id, typ, String.valueOf(d), c));
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
