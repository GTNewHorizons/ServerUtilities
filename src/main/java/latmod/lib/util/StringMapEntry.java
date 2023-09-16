package latmod.lib.util;

import java.util.AbstractMap;

import latmod.lib.IIDObject;

/**
 * Created by LatvianModder on 06.03.2016.
 */
public class StringMapEntry<E> extends AbstractMap.SimpleEntry<String, E>
        implements Comparable<StringMapEntry<E>>, IIDObject {

    public StringMapEntry(String k, E v) {
        super(k, v);
    }

    public int compareTo(StringMapEntry<E> o) {
        return getKey().compareToIgnoreCase(o.getKey());
    }

    public String getID() {
        return getKey();
    }
}
