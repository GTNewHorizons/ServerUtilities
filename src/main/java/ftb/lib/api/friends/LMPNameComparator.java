package ftb.lib.api.friends;

import java.util.Comparator;

public class LMPNameComparator implements Comparator<ILMPlayer> {

    public static final LMPNameComparator instance = new LMPNameComparator();

    public int compare(ILMPlayer o1, ILMPlayer o2) {
        return o1.getProfile().getName().compareToIgnoreCase(o2.getProfile().getName());
    }
}
