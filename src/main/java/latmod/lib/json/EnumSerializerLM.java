package latmod.lib.json;

import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class EnumSerializerLM {

    public static JsonElement serialize(Enum e) {
        return new JsonPrimitive(lowerCaseName(e));
    }

    public static <E extends Enum<E>> E deserialize(Class<?> type, JsonElement e) {
        if (!type.isEnum() || e == null || !e.isJsonPrimitive()) return null;
        else {
            String id = e.getAsString();
            Object[] o = type.getEnumConstants();

            for (Object anO : o) {
                if (lowerCaseName(anO).equals(id)) return (E) anO;
            }
        }
        return null;
    }

    public static String lowerCaseName(Object o) {
        return o instanceof Enum ? ((Enum<?>) o).name().toLowerCase(Locale.US) : o.toString().toLowerCase(Locale.US);
    }
}
