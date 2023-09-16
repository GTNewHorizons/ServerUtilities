package latmod.lib;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by LatvianModder on 06.03.2016.
 */
public class IntMap {

    public final IntList list;

    public IntMap(int s) {
        list = new IntList(s * 2);
    }

    public IntMap() {
        this(0);
    }

    public int size() {
        return list.size() / 2;
    }

    public String toString() {
        if (list.isEmpty()) return "{ }";
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append(' ');

        for (int i = 0; i < list.size(); i += 2) {
            sb.append(list.get(i));
            sb.append('=');
            sb.append(list.get(i + 1));

            if (i != list.size() - 2) {
                sb.append(',');
                sb.append(' ');
            }
        }

        sb.append(' ');
        sb.append('}');
        return sb.toString();
    }

    public int[] toArray() {
        return list.toArray();
    }

    public void fromArray(int[] ai) {
        list.clear();
        list.addAll(ai);
    }

    private int keyIndex(int key) {
        for (int i = 0; i < list.size(); i += 2) {
            if (list.get(i) == key) return i;
        }

        return -1;
    }

    public void put(int key, int value) {
        int index = keyIndex(key);
        if (index != -1) {
            list.set(index + 1, value);
        } else {
            list.add(key);
            list.add(value);
        }
    }

    public int get(int key) {
        return list.get(keyIndex(key) + 1);
    }

    public Map<Integer, Integer> toMap() {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < list.size(); i += 2) {
            map.put(list.get(i), list.get(i + 1));
        }
        return map;
    }

    public static IntMap fromMap(Map<Integer, Integer> map) {
        if (map == null) return null;
        else if (map.isEmpty()) return new IntMap();
        else {
            IntMap m = new IntMap(map.size());
            for (Map.Entry<Integer, Integer> e : map.entrySet()) {
                m.put(e.getKey(), e.getValue());
            }
            return m;
        }
    }

    public boolean containsKey(int key) {
        return keyIndex(key) != -1;
    }

    public void clear() {
        list.clear();
    }

    public IntList getKeys() {
        IntList list1 = new IntList(list.size() / 2);
        if (list.isEmpty()) return list1;

        for (int i = 0; i < list.size(); i += 2) {
            list1.add(list.get(i));
        }

        return list1;
    }

    public IntList getValues() {
        IntList list1 = new IntList(list.size() / 2);
        if (list.isEmpty()) return list1;

        for (int i = 0; i < list.size(); i += 2) {
            list1.add(list.get(i + 1));
        }

        return list1;
    }
}
