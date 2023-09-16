package latmod.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by LatvianModder on 06.01.2016.
 */
public class LMMapUtils {

    public static String toString(Map<?, ?> map) {
        if (map == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append(' ');

        int s = map.size();

        if (s > 0) {
            int i = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                sb.append(e.getKey());
                sb.append(':');
                sb.append(' ');
                sb.append(e.getValue());

                if (i != s - 1) {
                    sb.append(',');
                    sb.append(' ');
                }
                i++;
            }

            sb.append(' ');
        }

        sb.append('}');
        return sb.toString();
    }

    public static Map<String, String> toStringMap(Map<?, ?> map) {
        if (map == null) return null;
        HashMap<String, String> map1 = new HashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) map1.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
        return map1;
    }

    public static <K, V> Map<V, K> inverse(Map<K, V> map) {
        if (map == null) return null;
        HashMap<V, K> map1 = new HashMap<>();
        for (Map.Entry<K, V> e : map.entrySet()) map1.put(e.getValue(), e.getKey());
        return map1;
    }

    public static <K, V> List<Map.Entry<K, V>> sortedEntryList(Map<K, V> map, Comparator<Map.Entry<K, V>> c) {
        if (map == null) return null;
        ArrayList<Map.Entry<K, V>> list = new ArrayList<>();

        if (c == null) c = new Comparator<Map.Entry<K, V>>() {

            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return ((Comparable) o1.getKey()).compareTo(o2.getKey());
            }
        };

        list.addAll(map.entrySet());
        Collections.sort(list, c);
        return list;
    }

    public static <K, V> List<V> values(Map<K, V> map, Comparator<Map.Entry<K, V>> c) {
        if (map == null) return null;
        ArrayList<V> list = new ArrayList<>();
        for (Map.Entry<?, V> entry : sortedEntryList(map, c)) list.add(entry.getValue());
        return list;
    }

    public static <K, V> Comparator<Map.Entry<K, V>> byKeyNames(final boolean ignoreCase) {
        return new Comparator<Map.Entry<K, V>>() {

            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                if (ignoreCase) return String.valueOf(o1.getKey()).compareToIgnoreCase(String.valueOf(o2.getKey()));
                else return String.valueOf(o1.getKey()).compareTo(String.valueOf(o2.getKey()));
            }
        };
    }

    public static <K, V> Comparator<Map.Entry<K, V>> byKeyNumbers() {
        return new Comparator<Map.Entry<K, V>>() {

            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                Number n1 = ((Number) o1.getKey());
                Number n2 = ((Number) o1.getKey());
                return Long.compare((n1 == null) ? 0L : n1.longValue(), (n2 == null) ? 0L : n2.longValue());
            }
        };
    }

    public static <K, V> void removeAll(Map<K, V> map, RemoveFilter<Map.Entry<K, V>> f) {
        if (map == null) return;
        if (f == null) map.clear();
        else {
            List<Map.Entry<K, V>> set = new ArrayList<>(map.entrySet());
            map.clear();

            for (Map.Entry<K, V> e : set) {
                if (!f.remove(e)) map.put(e.getKey(), e.getValue());
            }
        }
    }

    public static <K, V> void sortMap(LinkedHashMap<K, V> map, Comparator<Map.Entry<K, V>> comparator) {
        if (map == null || map.isEmpty()) return;
        List<Map.Entry<K, V>> list = new ArrayList<>();
        list.addAll(map.entrySet());
        Collections.sort(list, comparator);
        map.clear();

        for (Map.Entry<K, V> e : list) {
            map.put(e.getKey(), e.getValue());
        }
    }
}
