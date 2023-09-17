package serverutils.lib.api.notification;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.IChatComponent;
import net.minecraft.util.IJsonSerializable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import serverutils.lib.JsonHelper;

public class MouseAction implements IJsonSerializable {

    public ClickAction click;
    public final List<IChatComponent> hover;

    public MouseAction() {
        hover = new ArrayList<>();
    }

    public JsonElement getSerializableElement() {
        JsonObject o = new JsonObject();

        if (click != null) o.add("click", click.getSerializableElement());

        if (!hover.isEmpty()) {
            JsonArray h = new JsonArray();
            for (IChatComponent c : hover) h.add(JsonHelper.serializeICC(c));
            o.add("hover", h);
        }

        return o;
    }

    public void func_152753_a(JsonElement e) {
        if (e == null || !e.isJsonObject()) return;

        JsonObject o1 = e.getAsJsonObject();

        if (o1.has("click")) {
            click = new ClickAction();
            click.func_152753_a(o1.get("click"));
        } else click = null;

        if (o1.has("hover")) {
            JsonArray a = o1.get("hover").getAsJsonArray();
            hover.clear();
            for (int i = 0; i < a.size(); i++) hover.add(JsonHelper.deserializeICC(a.get(i)));
        }
    }

    public void addHoverText(List<String> l) {
        for (IChatComponent c : hover) {
            if (c != null) l.add(c.getFormattedText());
            else l.add("");
        }
    }
}
