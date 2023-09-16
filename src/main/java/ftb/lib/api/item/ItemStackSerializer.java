package ftb.lib.api.item;

import net.minecraft.item.ItemStack;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import cpw.mods.fml.common.registry.GameRegistry;
import latmod.lib.LMStringUtils;

public class ItemStackSerializer {

    private static String getParseRegex(String s) {
        if (s.indexOf(' ') != -1) return " ";
        else if (s.indexOf(';') != -1) return ";";
        else if (s.indexOf('@') != -1) return "@";
        else return " x ";
    }

    public static ItemStack parseItem(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        String[] s1 = s.split(getParseRegex(s));
        if (s1.length <= 0) return null;

        String itemID = s1[0];
        int dmg = 0;
        int size = 1;
        String nbt = null;

        if (s1.length > 1) {
            size = Integer.parseInt(s1[1]);
        }

        if (s1.length > 2) {
            dmg = (s1[2].charAt(0) == '*') ? ODItems.ANY : Integer.parseInt(s1[2]);
        }

        if (s1.length > 3) {
            nbt = LMStringUtils.unsplitSpaceUntilEnd(3, s1);
        }

        ItemStack newStack = GameRegistry.makeItemStack(itemID, dmg, size, nbt);
        newStack.stackSize = size;
        return newStack;
    }

    public static String toString(ItemStack is) {
        if (is == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(LMInvUtils.getRegName(is));
        sb.append(' ');
        sb.append(is.stackSize);
        sb.append(' ');
        sb.append(is.getItemDamage());
        return sb.toString();
    }

    public static JsonElement serialize(ItemStack is) {
        if (is == null || is.getItem() == null) return null;
        return new JsonPrimitive(toString(is));
    }

    public static ItemStack deserialize(JsonElement e) {
        if (e == null) return null;
        else if (e.isJsonPrimitive()) return parseItem(e.getAsString());
        return null;
    }
}
