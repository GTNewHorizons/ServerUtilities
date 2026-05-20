package serverutils.client.tab;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

public class TabSkinCache {

    public static final TabSkinCache INSTANCE = new TabSkinCache();

    private final Map<String, ResourceLocation> cache = new HashMap<>();

    private TabSkinCache() {}

    public ResourceLocation getOrLoadSkin(String playerName) {
        ResourceLocation loc = cache.get(playerName);
        if (loc == null) {
            loc = AbstractClientPlayer.getLocationSkin(playerName);
            AbstractClientPlayer.getDownloadImageSkin(loc, playerName);
            cache.put(playerName, loc);
        }
        return loc;
    }

    public void cleanup(Collection<String> activePlayers) {
        Iterator<String> it = cache.keySet().iterator();
        while (it.hasNext()) {
            if (!activePlayers.contains(it.next())) {
                it.remove();
            }
        }
    }

    public void clear() {
        cache.clear();
    }
}
