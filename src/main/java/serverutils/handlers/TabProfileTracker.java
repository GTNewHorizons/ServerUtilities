package serverutils.handlers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import serverutils.lib.util.ServerUtils;
import serverutils.net.MessageTabProfiles;

/**
 * Tells clients which UUID/skin belongs to each tab list name, since the vanilla 1.7.10 tab packet only carries a name,
 * which hybrid servers replace with the Bukkit player list name (nickname/prefix).
 */
public class TabProfileTracker {

    private static final int CHECK_INTERVAL = 20;
    private static final Map<UUID, String> SENT_LIST_NAMES = new HashMap<>();
    private static final Field LIST_NAME_FIELD = findListNameField();
    private static int ticks = 0;

    private static Field findListNameField() {
        try {
            Field field = EntityPlayerMP.class.getDeclaredField("listName");
            if (field.getType() != String.class) return null;
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getListName(EntityPlayerMP player) {
        if (LIST_NAME_FIELD != null) {
            try {
                String name = (String) LIST_NAME_FIELD.get(player);
                if (name != null && !name.isEmpty()) return name;
            } catch (Exception ignored) {}
        }
        return player.getCommandSenderName();
    }

    private static MessageTabProfiles.Entry createEntry(EntityPlayerMP player, String listName) {
        GameProfile profile = player.getGameProfile();
        String textures = null;
        String signature = null;
        for (Property property : profile.getProperties().get("textures")) {
            textures = property.getValue();
            signature = property.getSignature();
            break;
        }
        return new MessageTabProfiles.Entry(listName, profile.getId(), profile.getName(), textures, signature);
    }

    public static void sendAllTo(MinecraftServer server, EntityPlayerMP target) {
        List<MessageTabProfiles.Entry> entries = new ArrayList<>();
        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            if (ServerUtils.isFake(player)) continue;
            entries.add(createEntry(player, getListName(player)));
        }
        if (!entries.isEmpty()) {
            new MessageTabProfiles(entries).sendTo(target);
        }
    }

    public static void tick(MinecraftServer server) {
        if (++ticks < CHECK_INTERVAL) return;
        ticks = 0;

        List<MessageTabProfiles.Entry> changed = new ArrayList<>();
        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            if (ServerUtils.isFake(player)) continue;
            String listName = getListName(player);
            String previous = SENT_LIST_NAMES.put(player.getUniqueID(), listName);
            if (!listName.equals(previous)) {
                changed.add(createEntry(player, listName));
            }
        }
        if (!changed.isEmpty()) {
            new MessageTabProfiles(changed).sendToAll();
        }
    }

    public static void remove(UUID id) {
        SENT_LIST_NAMES.remove(id);
    }

    public static void clear() {
        SENT_LIST_NAMES.clear();
        ticks = 0;
    }
}
