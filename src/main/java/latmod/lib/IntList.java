package latmod.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;

public class IntList implements Iterable<Integer> {

    private int defVal = -1;
    private int array[];
    private int size;

    public IntList(int i) {
        array = new int[i];
    }

    public IntList() {
        this(0);
    }

    public IntList(int[] ai) {
        if (ai != null && ai.length > 0) {
            size = ai.length;
            array = new int[size];
            System.arraycopy(ai, 0, array, 0, size);
        } else {
            size = 0;
            array = new int[0];
        }
    }

    public int size() {
        return size;
    }

    public void clear() {
        if (size > 0) {
            size = 0;
            array = new int[0];
        }
    }

    public IntList setDefVal(int value) {
        defVal = value;
        return this;
    }

    public void expand(int s) {
        if (size + s > array.length) {
            int ai[] = new int[size + Math.max(s, 10)];
            System.arraycopy(array, 0, ai, 0, size);
            array = ai;
        }
    }

    public void add(int value) {
        expand(1);
        array[size] = value;
        size++;
    }

    public void addAll(int... values) {
        if (values != null && values.length > 0) {
            expand(values.length);
            System.arraycopy(values, 0, array, size, values.length);
            size += values.length;
        }
    }

    public void addAll(IntList l) {
        if (l != null && l.size > 0) {
            expand(l.size);
            System.arraycopy(l.array, 0, array, size, l.size);
            size += l.size;
        }
    }

    public int get(int index) {
        return (index >= 0 && index < size) ? array[index] : defVal;
    }

    public int indexOf(int value) {
        if (size == 0) return -1;
        for (int i = 0; i < size; i++) if (array[i] == value) return i;
        return -1;
    }

    public boolean contains(int value) {
        return indexOf(value) != -1;
    }

    public int removeKey(int key) {
        if (key < 0 || key >= size) return defVal;
        int rem = get(key);
        size--;
        System.arraycopy(array, key + 1, array, key, size - key);
        return rem;
    }

    public int removeValue(int value) {
        return removeKey(indexOf(value));
    }

    public void set(int i, int value) {
        array[i] = value;
    }

    public boolean isEmpty() {
        return size <= 0;
    }

    public int[] toArray() {
        return toArray(null);
    }

    public int[] toArray(int[] a) {
        if (a == null || a.length != size) a = new int[size];
        if (size > 0) System.arraycopy(array, 0, a, 0, size);
        return a;
    }

    public List<Integer> toList() {
        ArrayList<Integer> l = new ArrayList<>();
        if (size == 0) return l;
        for (int i = 0; i < size; i++) l.add(array[i]);
        return l;
    }

    public void sort() {
        if (size < 2) return;
        Arrays.sort(array, 0, size);
    }

    public int[] toSortedArray() {
        if (size == 0) return new int[0];
        int[] a = toArray();
        Arrays.sort(a);
        return a;
    }

    public int hashCode() {
        int h = 0;
        for (int i = 0; i < size; i++) h = h * 31 + array[i];
        return h;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        else if (o == this) return true;
        else {
            IntList l = (IntList) o;
            if (size != l.size) return false;
            for (int i = 0; i < size; i++) {
                if (array[i] != l.array[i]) return false;
            }
            return true;
        }
    }

    public boolean equalsIgnoreOrder(IntList l) {
        if (l == null) return false;
        else if (l == this) return true;
        else {
            if (size != l.size) return false;

            IntList l1 = l.copy();

            for (int i = 0; i < size; i++) l1.removeValue(array[i]);

            return l1.isEmpty();
        }
    }

    public String toString() {
        if (size == 0) return "[ ]";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(' ');

        for (int i = 0; i < size; i++) {
            sb.append(array[i]);

            if (i != size - 1) {
                sb.append(',');
                sb.append(' ');
            }
        }

        sb.append(' ');
        sb.append(']');
        return sb.toString();
    }

    public Iterator<Integer> iterator() {
        return new IntIterator(array);
    }

    public IntList copy() {
        IntList l = new IntList(size);
        System.arraycopy(array, 0, l.array, 0, size);
        l.size = size;
        l.defVal = defVal;
        return l;
    }

    // Value, IsInNewList
    public Map<Integer, Boolean> getDifferenceMap(IntList newList) {
        HashMap<Integer, Boolean> map = new HashMap<>();

        if (isEmpty() && newList.isEmpty()) return map;

        for (int i = 0; i < size; i++) {
            if (!newList.contains(array[i])) map.put(array[i], false);
        }

        for (int i = 0; i < newList.size; i++) {
            if (!contains(newList.array[i])) map.put(array[i], true);
        }

        return map;
    }

    public static IntList asList(int... values) {
        IntList l = new IntList(values.length);
        l.addAll(values);
        return l;
    }

    public void setJson(JsonElement e) {
        clear();
        addAll(LMJsonUtils.fromIntArray(e));
    }

    public JsonElement getJson() {
        return LMJsonUtils.toIntArray(array);
    }

    public static class IntIterator implements Iterator<Integer> {

        public final int[] values;
        public int pos = -1;

        public IntIterator(int[] v) {
            values = v;
        }

        public boolean hasNext() {
            return pos < values.length;
        }

        public Integer next() {
            return values[++pos];
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
