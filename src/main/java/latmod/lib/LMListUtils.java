package latmod.lib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by LatvianModder on 06.01.2016.
 */
public class LMListUtils {

    public static String toString(Collection<?> c) {
        String[] s = toStringArray(c);
        if (s == null) return null;
        if (s.length == 0) return "[ ]";
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(' ');

        for (int i = 0; i < s.length; i++) {
            sb.append(s[i]);

            if (i != s.length - 1) {
                sb.append(',');
                sb.append(' ');
            }
        }

        sb.append(' ');
        sb.append(']');
        return sb.toString();
    }

    public static String[] toStringArray(Collection<?> c) {
        if (c == null) return null;
        String[] s = new String[c.size()];
        if (s.length == 0) return s;
        int i = -1;
        for (Object o : c) s[++i] = String.valueOf(o);
        return s;
    }

    public static List<String> toStringList(Collection<?> c) {
        if (c == null) return null;
        List<String> list = new ArrayList<>(c.size());
        if (c.isEmpty()) return list;
        for (Object o : c) list.add(String.valueOf(o));
        return list;
    }

    public static int[] toHashCodeArray(Collection<?> c) {
        if (c == null) return null;
        int[] s = new int[c.size()];
        int i = -1;
        for (Object o : c) s[++i] = LMUtils.hashCodeOf(o);
        return s;
    }

    public <E> List<E> flip(List<E> list) {
        if (list == null || list.isEmpty()) return list;
        int s = list.size();
        ArrayList<E> al1 = new ArrayList<>(s);
        for (int i = 0; i < s; i++) al1.add(list.get(s - i - 1));
        return al1;
    }

    public static void removeNullValues(List<?> list) {
        if (list == null) return;
        for (int i = list.size() - 1; i >= 0; i--) if (list.get(i) == null) list.remove(i);
    }

    public static void removeAll(List<?> list, IntList l) {
        if (list == null) return;
        for (int i = 0; i < l.size(); i++) list.remove(l.get(i));
    }

    public static <E> void removeAll(List<E> list, RemoveFilter<E> f) {
        if (list == null) return;
        if (f == null) list.clear();
        else {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (f.remove(list.get(i))) list.remove(i);
            }
        }
    }

    public static <E> List<E> sortToNew(Collection<E> c, Comparator<? super E> comparator) {
        if (c == null) return null;
        else if (c.isEmpty()) return new ArrayList<>();
        ArrayList<E> list = new ArrayList<>(c.size());
        list.addAll(c);
        Collections.sort(list, comparator);
        return list;
    }

    public static boolean trim(List<?> list, int t) {
        if (list != null && list.size() > t) {
            while (list.size() > t) {
                list.remove(t);
                t--;
            }
            return true;
        }

        return false;
    }

    public static <E> List<E> clone(Collection<E> c) {
        if (c == null) return null;
        if (c.isEmpty()) return new ArrayList<>();
        ArrayList<E> list1 = new ArrayList<>(c.size());
        list1.addAll(c);
        return list1;
    }

    // TODO: Remove me
    public static <E> void addAll(Collection<E> c, E[] e) {
        if (c != null && e != null && e.length > 0) {
            Collections.addAll(c, e);
        }
    }

    public static boolean containsAny(Collection<?> c, Collection<?> c1) {
        for (Object o : c1) {
            if (c.contains(o)) return true;
        }

        return false;
    }
}
