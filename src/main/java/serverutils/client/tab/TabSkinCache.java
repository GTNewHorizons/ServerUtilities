package serverutils.client.tab;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;

import serverutils.ServerUtilities;
import serverutils.net.MessageTabProfiles;

public class TabSkinCache {

    public static final TabSkinCache INSTANCE = new TabSkinCache();

    private final Map<String, ResourceLocation> cache = new ConcurrentHashMap<>();
    private final Map<String, MessageTabProfiles.Entry> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, ResourceLocation> profileSkins = new ConcurrentHashMap<>();
    private final Set<UUID> noProfileSkin = ConcurrentHashMap.newKeySet();

    private TabSkinCache() {}

    public void registerProfile(MessageTabProfiles.Entry entry) {
        profiles.values().removeIf(e -> e.id.equals(entry.id) && !e.listName.equals(entry.listName));
        MessageTabProfiles.Entry old = profiles.put(entry.listName, entry);
        if (old == null || !old.id.equals(entry.id) || !old.textures.equals(entry.textures)) {
            profileSkins.remove(entry.id);
            noProfileSkin.remove(entry.id);
            cache.remove(entry.listName);
        }
    }

    public ResourceLocation getOrLoadSkin(String tabName) {
        MessageTabProfiles.Entry profile = profiles.get(tabName);
        if (profile != null) {
            ResourceLocation loc = getProfileSkin(profile);
            if (loc != null) return loc;
        }

        String lookupName = profile != null ? profile.name : tabName;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            EntityPlayer entity = profile != null ? mc.theWorld.func_152378_a(profile.id)
                    : mc.theWorld.getPlayerEntityByName(tabName);
            if (entity instanceof AbstractClientPlayer acp) {
                ResourceLocation live = acp.getLocationSkin();
                if (live != null) {
                    cache.put(tabName, live);
                    return live;
                }
            }
        }

        ResourceLocation loc = cache.get(tabName);
        if (loc == null) {
            loc = AbstractClientPlayer.getLocationSkin(lookupName);
            AbstractClientPlayer.getDownloadImageSkin(loc, lookupName);
            cache.put(tabName, loc);
        }
        return loc;
    }

    private ResourceLocation getProfileSkin(MessageTabProfiles.Entry profile) {
        if (profile.textures.isEmpty() || noProfileSkin.contains(profile.id)) return null;
        ResourceLocation loc = profileSkins.get(profile.id);
        if (loc != null) return loc;

        try {
            GameProfile gameProfile = new GameProfile(profile.id, profile.name);
            gameProfile.getProperties().put(
                    "textures",
                    profile.signature.isEmpty() ? new Property("textures", profile.textures)
                            : new Property("textures", profile.textures, profile.signature));
            Minecraft mc = Minecraft.getMinecraft();
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = mc.func_152347_ac()
                    .getTextures(gameProfile, false);
            MinecraftProfileTexture skin = textures.get(MinecraftProfileTexture.Type.SKIN);
            if (skin == null) {
                noProfileSkin.add(profile.id);
                return null;
            }
            loc = mc.func_152342_ad().func_152789_a(skin, MinecraftProfileTexture.Type.SKIN, null);
        } catch (Exception e) {
            ServerUtilities.LOGGER.debug("Failed to load tab skin for {}", profile.name, e);
            noProfileSkin.add(profile.id);
            return null;
        }
        profileSkins.put(profile.id, loc);
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
        profiles.clear();
        profileSkins.clear();
        noProfileSkin.clear();
    }
}
