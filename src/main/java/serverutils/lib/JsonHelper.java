package serverutils.lib;

import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumTypeAdapterFactory;
import net.minecraft.util.IChatComponent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class JsonHelper {

    public static Gson chatComponentGson;

    public static void init() {
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeHierarchyAdapter(IChatComponent.class, new IChatComponent.Serializer());
        gb.registerTypeHierarchyAdapter(ChatStyle.class, new ChatStyle.Serializer());
        gb.registerTypeAdapterFactory(new EnumTypeAdapterFactory());
        chatComponentGson = gb.create();
    }

    public static JsonElement serializeICC(IChatComponent c) {
        if (c == null) return JsonNull.INSTANCE;
        return chatComponentGson.toJsonTree(c, IChatComponent.class);
    }

    public static IChatComponent deserializeICC(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        return chatComponentGson.fromJson(e, IChatComponent.class);
    }
    /*
     * public static JsonElement serializeNBT(NBTBase base) { if(base == null || base.getId() == 0) return
     * JsonNull.INSTANCE; throw new IllegalStateException("Unable to serialize NBTBase " + base); } public static
     * NBTBase deserializeNBT(JsonElement e) { if(e == null || e.isJsonNull()) return null; try { return
     * JsonToNBT.func_150315_a(e.toString()); } catch(Exception ex) { ex.printStackTrace(); } throw new
     * IllegalStateException("Unable to deserialize NBTBase " + e); }
     */
}
