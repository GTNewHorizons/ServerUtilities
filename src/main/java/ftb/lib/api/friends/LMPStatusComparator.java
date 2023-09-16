package ftb.lib.api.friends;

import java.util.Comparator;

public class LMPStatusComparator implements Comparator<ILMPlayer> {

    public final ILMPlayer self;

    public LMPStatusComparator(ILMPlayer p) {
        self = p;
    }

    public int compare(ILMPlayer p1, ILMPlayer p2) {
        int output = 0;

        FriendStatus f1 = FriendStatus.get(self, p1);
        FriendStatus f2 = FriendStatus.get(self, p2);

        boolean o1 = p1.isOnline();
        boolean o2 = p2.isOnline();

        if (f1 == f2) output = 0;
        else {
            if (f1 == FriendStatus.FRIEND) output = -1;
            else if (f2 == FriendStatus.FRIEND) output = 1;
            else {
                if (f1 == FriendStatus.NONE) return 1;
                else if (f2 == FriendStatus.NONE) return -1;
            }
        }

        if (output == 0) {
            if (o1 && !o2) output = -1;
            else if (!o1 && o2) output = 1;
            else if ((o1 && o2) || (!o1 && !o2)) output = 0;
            if (output == 0) output = p1.getProfile().getName().compareToIgnoreCase(p2.getProfile().getName());
        }

        return output;
    }
}
