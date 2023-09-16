package serverutils.lib.api.notification;

import net.minecraft.event.ClickEvent;
import net.minecraft.util.IJsonSerializable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by LatvianModder on 23.01.2016.
 */
public class ClickAction implements IJsonSerializable {

    public ClickActionType type;
    public JsonElement data;

    public ClickAction() {}

    public ClickAction(ClickActionType t, JsonElement d) {
        type = t;
        data = d;
    }

    public JsonElement getSerializableElement() {
        JsonObject o = new JsonObject();
        o.add("type", new JsonPrimitive(type.getID()));
        if (data != null) o.add("data", data);
        return o;
    }

    public void func_152753_a(JsonElement e) {
        JsonObject o = e.getAsJsonObject();
        type = ClickActionRegistry.get(o.get("type").getAsString());
        data = o.get("data");
    }

    @SideOnly(Side.CLIENT)
    public void onClicked() {
        if (type != null) type.onClicked(data);
    }

    public static ClickAction from(ClickEvent e) {
        if (e != null) {
            JsonPrimitive p = new JsonPrimitive(e.getValue());

            switch (e.getAction()) {
                case RUN_COMMAND:
                    return new ClickAction(ClickActionType.CMD, p);
                case OPEN_FILE:
                    return new ClickAction(ClickActionType.FILE, p);
                case SUGGEST_COMMAND:
                    return new ClickAction(ClickActionType.SHOW_CMD, p);
                case OPEN_URL:
                    return new ClickAction(ClickActionType.URL, p);
            }
        }
        return null;
    }
}
