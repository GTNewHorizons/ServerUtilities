package serverutils.lib.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.util.Constants;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import serverutils.lib.ATHelper;
import serverutils.lib.math.Ticks;
import serverutils.lib.util.text_components.ChatComponentScore;
import serverutils.lib.util.text_components.Notification;
import serverutils.lib.util.text_components.TextComponentCountdown;

public class JsonUtils {

    public static final JsonPrimitive JSON_TRUE = new JsonPrimitive(true);
    public static final JsonPrimitive JSON_FALSE = new JsonPrimitive(false);
    public static final JsonPrimitive JSON_EMPTY_STRING = new JsonPrimitive("");
    public static final JsonPrimitive JSON_ZERO = new JsonPrimitive(0);

    public static boolean isNull(@Nullable JsonElement element) {
        return element == null || element == JsonNull.INSTANCE || element.isJsonNull();
    }

    public static JsonElement nonnull(@Nullable JsonElement json) {
        return isNull(json) ? JsonNull.INSTANCE : json;
    }

    public static JsonElement parse(@Nullable Reader reader) throws Exception {
        if (reader == null) {
            return JsonNull.INSTANCE;
        }

        JsonReader jsonReader = new JsonReader(reader);
        JsonElement element;
        boolean lenient = jsonReader.isLenient();
        jsonReader.setLenient(true);
        element = Streams.parse(jsonReader);

        if (!element.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
            throw new JsonSyntaxException("Did not consume the entire document.");
        }

        return element;
    }

    public static void toJson(Writer writer, @Nullable JsonElement element, boolean prettyPrinting) {
        if (isNull(element)) {
            try {
                writer.write("null");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return;
        }

        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setLenient(true);
        jsonWriter.setHtmlSafe(false);
        jsonWriter.setSerializeNulls(true);

        if (prettyPrinting) {
            jsonWriter.setIndent("\t");
        }

        try {
            Streams.write(element, jsonWriter);
        } catch (Exception ex) {
            throw new JsonIOException(ex);
        }
    }

    public static String toJson(@Nullable JsonElement element, boolean prettyPrinting) {
        StringWriter writer = new StringWriter();
        toJson(writer, element, prettyPrinting);
        return writer.toString();
    }

    public static void toJson(File file, @Nullable JsonElement element, boolean prettyPrinting) {
        try (OutputStreamWriter output = new OutputStreamWriter(
                new FileOutputStream(FileUtils.newFile(file)),
                StandardCharsets.UTF_8); BufferedWriter writer = new BufferedWriter(output)) {
            toJson(writer, element, prettyPrinting);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String toJson(@Nullable JsonElement element) {
        return toJson(element, false);
    }

    public static void toJson(File file, @Nullable JsonElement element) {
        toJson(file, element, true);
    }

    public static void toJsonSafe(final File file, final @Nullable JsonElement element) {
        ThreadedFileIOBase.threadedIOInstance.queueIO(() -> {
            toJson(file, element);
            return false;
        });
    }

    public static JsonArray toArray(JsonElement element) {
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }

        JsonArray array = new JsonArray();

        if (!element.isJsonNull()) {
            array.add(element);
        }

        return array;
    }

    public static JsonElement serializeTextComponent(@Nullable IChatComponent component) {
        if (component == null) {
            return JsonNull.INSTANCE;
        } else if (component.getClass() == ChatComponentText.class && component.getSiblings().isEmpty()
                && component.getChatStyle().isEmpty()) {
                    return new JsonPrimitive(((ChatComponentText) component).getChatComponentText_TextValue());
                }

        JsonObject json = new JsonObject();
        ChatStyle style = component.getChatStyle();

        if (!style.isEmpty()) {
            if (ATHelper.getBold(style) != null) {
                json.addProperty("bold", style.getBold());
            }

            if (ATHelper.getItalic(style) != null) {
                json.addProperty("italic", style.getItalic());
            }

            if (ATHelper.getUnderlined(style) != null) {
                json.addProperty("underlined", style.getUnderlined());
            }

            if (ATHelper.getStrikethrough(style) != null) {
                json.addProperty("strikethrough", style.getStrikethrough());
            }

            if (ATHelper.getObfuscated(style) != null) {
                json.addProperty("obfuscated", style.getObfuscated());
            }

            if (ATHelper.getColor(style) != null) {
                json.addProperty("color", style.getColor().getFriendlyName());
            }

            if (ATHelper.getClickEvent(style) != null) {
                json.add("clickEvent", serializeClickEvent(style.getChatClickEvent()));
            }

            if (ATHelper.getHoverEvent(style) != null) {
                json.add("hoverEvent", serializeHoverEvent(style.getChatHoverEvent()));
            }
        }

        if (!component.getSiblings().isEmpty()) {
            JsonArray array = new JsonArray();

            for (IChatComponent IChatComponent : component.getSiblings()) {
                array.add(serializeTextComponent(IChatComponent));
            }

            json.add("extra", array);
        }

        if (component instanceof ChatComponentText text) {
            json.addProperty("text", text.getChatComponentText_TextValue());

            if (component instanceof Notification n) {

                if (!n.getId().equals(Notification.VANILLA_STATUS)) {
                    json.addProperty("notification", n.getId().toString());
                }

                if (n.getTimer().ticks() != 60) {
                    json.addProperty("timer", n.getTimer().toString());
                }

                if (n.isImportant()) {
                    json.addProperty("important", true);
                }

                if (n.isVanilla()) {
                    json.addProperty("vanilla", true);
                }
            }
        } else if (component instanceof ChatComponentTranslation translation) {
            json.addProperty("translate", translation.getKey());

            if (translation.getFormatArgs().length > 0) {
                JsonArray array = new JsonArray();

                for (Object object : translation.getFormatArgs()) {
                    if (object instanceof IChatComponent) {
                        array.add(serializeTextComponent((IChatComponent) object));
                    } else {
                        array.add(new JsonPrimitive(String.valueOf(object)));
                    }
                }

                json.add("with", array);
            }
        }

        if (json.entrySet().isEmpty()) {
            throw new IllegalArgumentException("Don't know how to serialize " + component + " as a Component");
        }

        return json;
    }

    @Nullable
    public static IChatComponent deserializeTextComponent(@Nullable JsonElement element) {
        if (isNull(element)) {
            return null;
        } else if (element.isJsonPrimitive()) {
            return new ChatComponentText(StringUtils.fixTabs(element.getAsString(), 2));
        } else if (!element.isJsonObject()) {
            if (!element.isJsonArray()) {
                throw new JsonParseException("Don't know how to turn " + element + " into a Component");
            }

            IChatComponent component = null;

            for (JsonElement element1 : element.getAsJsonArray()) {
                IChatComponent component1 = deserializeTextComponent(element1);

                if (component1 == null) {
                    component1 = new ChatComponentText("");
                }

                if (component == null) {
                    component = component1;
                } else {
                    component.appendSibling(component1);
                }
            }

            return component;
        } else {
            JsonObject json = element.getAsJsonObject();
            IChatComponent component;

            if (json.has("text") || json.has("notification")) {
                String s = json.has("text") ? StringUtils.fixTabs(json.get("text").getAsString(), 2) : "";

                if (json.has("notification") || json.has("timer") || json.has("important") || json.has("vanilla")) {
                    Notification n = Notification.of(
                            json.has("notification") ? new ResourceLocation(json.get("notification").getAsString())
                                    : Notification.VANILLA_STATUS,
                            s);
                    component = n;

                    if (json.has("timer")) {
                        JsonElement e = json.get("timer");

                        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
                            n.setTimer(Ticks.get(e.getAsLong()));
                        } else {
                            n.setTimer(Ticks.get(e.getAsString()));
                        }
                    }

                    if (json.has("important")) {
                        n.setImportant(net.minecraft.util.JsonUtils.getJsonObjectBooleanFieldValue(json, "important"));
                    }

                    if (json.has("vanilla")) {
                        n.setVanilla(net.minecraft.util.JsonUtils.getJsonObjectBooleanFieldValue(json, "vanilla"));
                    }
                } else if (json.has("countdown")) {
                    component = new TextComponentCountdown(s, json.get("countdown").getAsLong());
                } else {
                    component = new ChatComponentText(s);
                }
            } else if (json.has("translate")) {
                String s = json.get("translate").getAsString();

                if (json.has("with")) {
                    JsonArray a = json.getAsJsonArray("with");
                    Object[] o1 = new Object[a.size()];

                    for (int i = 0; i < o1.length; ++i) {
                        o1[i] = deserializeTextComponent(a.get(i));

                        if (o1[i] != null && o1[i].getClass() == ChatComponentText.class) {
                            ChatComponentText t2 = (ChatComponentText) o1[i];

                            if (t2.getChatStyle().isEmpty() && t2.getSiblings().isEmpty()) {
                                o1[i] = t2.getChatComponentText_TextValue();
                            }
                        }
                    }

                    component = new ChatComponentTranslation(s, o1);
                } else {
                    component = new ChatComponentTranslation(s);
                }
            } else if (json.has("score")) {
                JsonObject o1 = json.getAsJsonObject("score");

                if (!o1.has("name") || !o1.has("objective")) {
                    throw new JsonParseException("A score component needs a least a name and an objective");
                }

                component = new ChatComponentScore(
                        net.minecraft.util.JsonUtils.getJsonObjectStringFieldValue(o1, "name"),
                        net.minecraft.util.JsonUtils.getJsonObjectStringFieldValue(o1, "objective"));

                if (o1.has("value")) {
                    ((ChatComponentScore) component)
                            .setValue(net.minecraft.util.JsonUtils.getJsonObjectStringFieldValue(o1, "value"));
                }
                // } else if (json.has("selector")) {
                // component = new
                // TextComponentSelector(net.minecraft.util.JsonUtils.getJsonObjectStringFieldValue(json, "selector"));
                // } else if (json.has("keybind")) {
                // component = new TextComponentKeybind(net.minecraft.util.JsonUtils.getJsonObjectStringFieldValue(json,
                // "keybind"));
            } else {
                return null;
            }

            if (json.has("extra")) {
                JsonArray a = json.getAsJsonArray("extra");

                if (a.size() <= 0) {
                    throw new JsonParseException("Unexpected empty array of components");
                }

                for (JsonElement element1 : a) {
                    IChatComponent component1 = deserializeTextComponent(element1);
                    component.appendSibling(component1 == null ? new ChatComponentText("") : component1);
                }
            }

            ChatStyle style = new ChatStyle();

            if (json.has("bold")) {
                style.setBold(json.get("bold").getAsBoolean());
            }

            if (json.has("italic")) {
                style.setItalic(json.get("italic").getAsBoolean());
            }

            if (json.has("underlined")) {
                style.setUnderlined(json.get("underlined").getAsBoolean());
            }

            if (json.has("strikethrough")) {
                style.setStrikethrough(json.get("strikethrough").getAsBoolean());
            }

            if (json.has("obfuscated")) {
                style.setObfuscated(json.get("obfuscated").getAsBoolean());
            }

            if (json.has("color")) {
                style.setColor(EnumChatFormatting.getValueByName(json.get("color").getAsString()));
            }

            // if (json.has("insertion")) {
            // style.setInsertion(json.get("insertion").getAsString());
            // }

            if (json.has("clickEvent")) {
                style.setChatClickEvent(deserializeClickEvent(json.get("clickEvent")));
            }

            if (json.has("hoverEvent")) {
                style.setChatHoverEvent(deserializeHoverEvent(json.get("hoverEvent")));
            }

            component.setChatStyle(style);
            return component;
        }
    }

    public static JsonElement serializeClickEvent(@Nullable ClickEvent event) {
        if (event == null) {
            return JsonNull.INSTANCE;
        }

        JsonObject o = new JsonObject();
        o.addProperty("action", event.getAction().getCanonicalName());
        o.addProperty("value", event.getValue());
        return o;
    }

    @Nullable
    public static ClickEvent deserializeClickEvent(JsonElement element) {
        if (isNull(element)) {
            return null;
        } else if (element.isJsonPrimitive()) {
            return new ClickEvent(ClickEvent.Action.OPEN_URL, element.getAsString());
        }

        JsonObject o = element.getAsJsonObject();

        if (o != null) {
            JsonPrimitive a = o.getAsJsonPrimitive("action");
            ClickEvent.Action action = a == null ? null : ClickEvent.Action.getValueByCanonicalName(a.getAsString());
            JsonPrimitive v = o.getAsJsonPrimitive("value");
            String s = v == null ? null : v.getAsString();

            if (action != null && s != null && action.shouldAllowInChat()) {
                return new ClickEvent(action, s);
            }
        }

        return null;
    }

    public static JsonElement serializeHoverEvent(@Nullable HoverEvent event) {
        if (event == null) {
            return JsonNull.INSTANCE;
        }

        JsonObject o = new JsonObject();
        o.addProperty("action", event.getAction().getCanonicalName());
        o.add("value", serializeTextComponent(event.getValue()));
        return o;
    }

    @Nullable
    public static HoverEvent deserializeHoverEvent(@Nullable JsonElement element) {
        if (isNull(element)) {
            return null;
        }

        JsonObject o = element.getAsJsonObject();

        if (o != null) {
            JsonPrimitive a = o.getAsJsonPrimitive("action");
            HoverEvent.Action action = a == null ? null : HoverEvent.Action.getValueByCanonicalName(a.getAsString());
            IChatComponent t = deserializeTextComponent(o.get("value"));

            if (action != null && t != null && action.shouldAllowInChat()) {
                return new HoverEvent(action, t);
            }
        }

        return null;
    }

    public static JsonObject fromJsonTree(JsonObject o) {
        JsonObject map = new JsonObject();
        fromJsonTree0(map, null, o);
        return map;
    }

    private static void fromJsonTree0(JsonObject map, @Nullable String id0, JsonObject o) {
        for (Map.Entry<String, JsonElement> entry : o.entrySet()) {
            if (entry.getValue() instanceof JsonObject) {
                fromJsonTree0(
                        map,
                        (id0 == null) ? entry.getKey() : (id0 + '.' + entry.getKey()),
                        entry.getValue().getAsJsonObject());
            } else {
                map.add((id0 == null) ? entry.getKey() : (id0 + '.' + entry.getKey()), entry.getValue());
            }
        }
    }

    public static JsonObject toJsonTree(Collection<Map.Entry<String, JsonElement>> tree) {
        JsonObject o1 = new JsonObject();

        for (Map.Entry<String, JsonElement> entry : tree) {
            findGroup(o1, entry.getKey()).add(lastKeyPart(entry.getKey()), entry.getValue());
        }

        return o1;
    }

    private static String lastKeyPart(String s) {
        int idx = s.lastIndexOf('.');

        if (idx != -1) {
            return s.substring(idx + 1);
        }

        return s;
    }

    private static JsonObject findGroup(JsonObject parent, String s) {
        int idx = s.indexOf('.');

        if (idx != -1) {
            String s0 = s.substring(0, idx);

            JsonElement o = parent.get(s0);

            if (o == null) {
                o = new JsonObject();
                parent.add(s0, o);
            }

            return findGroup(o.getAsJsonObject(), s.substring(idx + 1, s.length() - 1));
        }

        return parent;
    }

    public static String fixJsonString(String json) {
        if (json.isEmpty()) {
            return "\"\"";
        }

        if (json.indexOf(' ') != -1
                && !((json.startsWith("\"") && json.endsWith("\"")) || (json.startsWith("{") && json.endsWith("}"))
                        || (json.startsWith("[") && json.endsWith("]")))) {
            json = "\"" + json + "\"";
        }

        return json;
    }

    public static JsonElement toJson(@Nullable NBTBase nbt) {
        if (nbt == null) {
            return JsonNull.INSTANCE;
        }

        switch (nbt.getId()) {
            case Constants.NBT.TAG_COMPOUND: {
                NBTTagCompound tagCompound = (NBTTagCompound) nbt;
                JsonObject json = new JsonObject();

                if (!tagCompound.hasNoTags()) {
                    for (String s : tagCompound.func_150296_c()) {
                        json.add(s, toJson(tagCompound.getTag(s)));
                    }
                }

                return json;
            }
            case Constants.NBT.TAG_LIST: {
                JsonArray json = new JsonArray();
                NBTTagList list = (NBTTagList) nbt;

                if (list.tagCount() != 0) {
                    for (int i = 0; i < list.tagCount(); i++) {
                        json.add(toJson((NBTBase) list.tagList.get(i)));

                    }
                }

                return json;
            }
            case Constants.NBT.TAG_STRING: {
                String s = ((NBTTagString) nbt).func_150285_a_();
                return s.isEmpty() ? JSON_EMPTY_STRING : new JsonPrimitive(s);
            }
            case Constants.NBT.TAG_BYTE:
                return new JsonPrimitive(((NBTTagByte) nbt).func_150290_f());
            case Constants.NBT.TAG_SHORT:
                return new JsonPrimitive(((NBTTagShort) nbt).func_150289_e());
            case Constants.NBT.TAG_INT:
                return new JsonPrimitive(((NBTTagInt) nbt).func_150287_d());
            case Constants.NBT.TAG_LONG:
                return new JsonPrimitive(((NBTTagLong) nbt).func_150291_c());
            case Constants.NBT.TAG_FLOAT:
                return new JsonPrimitive(((NBTTagFloat) nbt).func_150288_h());
            case Constants.NBT.TAG_DOUBLE:
                return new JsonPrimitive(((NBTTagDouble) nbt).func_150286_g());
            case Constants.NBT.TAG_BYTE_ARRAY: {
                JsonArray json = new JsonArray();
                NBTTagByteArray ba = (NBTTagByteArray) nbt;

                if (ba.func_150292_c().length != 0) {
                    for (byte v : ba.func_150292_c()) {
                        json.add(new JsonPrimitive(v));
                    }
                }

                return json;
            }
            case Constants.NBT.TAG_INT_ARRAY: {
                JsonArray json = new JsonArray();
                NBTTagIntArray ia = (NBTTagIntArray) nbt;

                if (ia.func_150302_c().length != 0) {
                    for (int v : ia.func_150302_c()) {
                        json.add(new JsonPrimitive(v));
                    }
                }

                return json;
            }
            default:
                return JsonNull.INSTANCE;
        }
    }

    @Nullable
    public static NBTBase toNBT(@Nullable JsonElement element) {
        if (isNull(element)) {
            return null;
        }

        try {
            return JsonToNBT.func_150315_a(toJson(element));
        } catch (Exception ex) {
            return null;
        }
    }

    public static JsonElement copy(JsonElement json) {
        if (isNull(json)) {
            return JsonNull.INSTANCE;
        } else if (json.isJsonObject()) {
            JsonObject json1 = new JsonObject();

            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                json1.add(entry.getKey(), copy(entry.getValue()));
            }

            return json1;
        } else if (json.isJsonArray()) {
            JsonArray json1 = new JsonArray();

            for (JsonElement element : json.getAsJsonArray()) {
                json1.add(copy(element));
            }

            return json1;
        }

        return json;
    }

    public static JsonElement fromJson(Reader json) {
        return (json == null) ? JsonNull.INSTANCE : new JsonParser().parse(json);
    }

    public static JsonElement fromJson(File json) {
        try {
            if (json == null || !json.exists()) return JsonNull.INSTANCE;
            BufferedReader reader = new BufferedReader(new FileReader(json));
            JsonElement e = fromJson(reader);
            reader.close();
            return e;
        } catch (Exception ex) {}
        return JsonNull.INSTANCE;
    }

    public static void copy(JsonObject from, JsonObject to) {
        for (Map.Entry<String, JsonElement> entry : from.entrySet()) {
            to.add(entry.getKey(), copy(entry.getValue()));
        }
    }
}
