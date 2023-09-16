package latmod.lib.json;

import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import latmod.lib.ByteIOStream;

/**
 * Created by LatvianModder on 23.01.2016.
 */
public class JsonElementIO {

    public enum JsonID {

        NULL,
        ARRAY,
        OBJECT,
        STRING,
        BOOL,
        BYTE,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE;

        public final byte ID;
        public final String name;

        JsonID() {
            ID = (byte) ordinal();
            name = name().toLowerCase();
        }
    }

    public static JsonID getID(JsonElement e) {
        if (e == null || e.isJsonNull()) return JsonID.NULL;
        else if (e.isJsonArray()) return JsonID.ARRAY;
        else if (e.isJsonObject()) return JsonID.OBJECT;
        else {
            JsonPrimitive p = e.getAsJsonPrimitive();

            if (p.isString()) return JsonID.STRING;
            else if (p.isBoolean()) return JsonID.BOOL;
            else {
                Number n = p.getAsNumber();

                System.out.println(n.getClass());

                if (n instanceof Integer) return JsonID.INT;
                else if (n instanceof Byte) return JsonID.BYTE;
                else if (n instanceof Short) return JsonID.SHORT;
                else if (n instanceof Long) return JsonID.LONG;
                else if (n instanceof Float) return JsonID.FLOAT;
                else if (n instanceof Double) return JsonID.DOUBLE;
                else return JsonID.NULL;
            }
        }
    }

    public static JsonElement read(ByteIOStream io) {
        switch (JsonID.values()[io.readByte()]) {
            case NULL:
                return JsonNull.INSTANCE;
            case ARRAY: {
                JsonArray a = new JsonArray();
                int s = io.readInt();

                for (int i = 0; i < s; i++) a.add(read(io));

                return a;
            }
            case OBJECT: {
                JsonObject o = new JsonObject();
                int s = io.readInt();

                for (int i = 0; i < s; i++) {
                    String key = io.readUTF();
                    o.add(key, read(io));
                }

                return o;
            }
            case STRING:
                return new JsonPrimitive(io.readUTF());
            case BOOL:
                return new JsonPrimitive(io.readBoolean());
            case BYTE:
                return new JsonPrimitive(io.readByte());
            case SHORT:
                return new JsonPrimitive(io.readShort());
            case INT:
                return new JsonPrimitive(io.readInt());
            case LONG:
                return new JsonPrimitive(io.readLong());
            case FLOAT:
                return new JsonPrimitive(io.readFloat());
            case DOUBLE:
                return new JsonPrimitive(io.readDouble());
        }

        return JsonNull.INSTANCE;
    }

    public static void write(ByteIOStream io, JsonElement e) {
        if (e == null || e.isJsonNull()) io.writeByte(JsonID.NULL.ID);
        else if (e.isJsonArray()) {
            io.writeByte(JsonID.ARRAY.ID);

            JsonArray a = e.getAsJsonArray();
            int s = a.size();
            io.writeInt(s);

            for (int i = 0; i < s; i++) write(io, a.get(i));
        } else if (e.isJsonObject()) {
            io.writeByte(JsonID.OBJECT.ID);

            Set<Map.Entry<String, JsonElement>> set = e.getAsJsonObject().entrySet();
            io.writeInt(set.size());

            for (Map.Entry<String, JsonElement> entry : set) {
                io.writeUTF(entry.getKey());
                write(io, entry.getValue());
            }
        } else {
            JsonPrimitive p = e.getAsJsonPrimitive();

            if (p.isString()) {
                io.writeByte(JsonID.STRING.ID);
                io.writeUTF(p.getAsString());
            } else if (p.isBoolean()) {
                io.writeByte(JsonID.BOOL.ID);
                io.writeBoolean(p.getAsBoolean());
            } else {
                Number n = p.getAsNumber();

                System.out.println(n.getClass());

                if (n instanceof Integer) {
                    io.writeByte(JsonID.INT.ID);
                    io.writeInt(n.intValue());
                } else if (n instanceof Byte) {
                    io.writeByte(JsonID.BYTE.ID);
                    io.writeByte(n.byteValue());
                } else if (n instanceof Short) {
                    io.writeByte(JsonID.SHORT.ID);
                    io.writeShort(n.shortValue());
                } else if (n instanceof Long) {
                    io.writeByte(JsonID.LONG.ID);
                    io.writeLong(n.longValue());
                } else if (n instanceof Float) {
                    io.writeByte(JsonID.FLOAT.ID);
                    io.writeFloat(n.floatValue());
                } else if (n instanceof Double) {
                    io.writeByte(JsonID.DOUBLE.ID);
                    io.writeDouble(n.doubleValue());
                } else io.writeByte(JsonID.NULL.ID);
            }
        }
    }
}
