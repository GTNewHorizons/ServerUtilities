package ftb.lib;

import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.FakePlayer;

public class LMAccessToken {

    private static final HashMap<UUID, Long> tokens = new HashMap<>();
    private static final Random random = new Random();

    public static long generate(EntityPlayerMP ep) {
        if (ep == null || ep instanceof FakePlayer) return 0L;
        long token = random.nextLong();
        tokens.put(ep.getUniqueID(), token);
        return token;
    }

    public static boolean equals(EntityPlayerMP ep, long token, boolean remove) {
        if (ep == null || ep instanceof FakePlayer) return false;

        Long t = tokens.get(ep.getUniqueID());

        if (t != null && t.longValue() == token) {
            if (remove) tokens.remove(ep.getUniqueID());
            return true;
        }

        return false;
    }

    public static void clear() {
        tokens.clear();
    }
}
