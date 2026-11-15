package serverutils.handlers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private static final Map<UUID, String> SENT_STATES = new HashMap<>();
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

    private static String getState(EntityPlayerMP player, MessageTabProfiles.Entry entry) {
        return entry.listName + '\n' + entry.textures + '\n' + entry.signature + '\n' + ServerUtils.isVanished(player);
    }

    public static void sendAllTo(MinecraftServer server, EntityPlayerMP target) {
        if (server == null) return;
        List<MessageTabProfiles.Entry> entries = new ArrayList<>();
        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            if (ServerUtils.isFake(player) || !ServerUtils.canSee(target, player)) continue;
            entries.add(createEntry(player, getListName(player)));
        }
        if (!entries.isEmpty()) {
            new MessageTabProfiles(entries).sendTo(target);
        }
    }

    public static void tick(MinecraftServer server) {
        if (++ticks < CHECK_INTERVAL) return;
        ticks = 0;

        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        Map<EntityPlayerMP, MessageTabProfiles.Entry> changed = new LinkedHashMap<>();
        for (EntityPlayerMP player : players) {
            if (ServerUtils.isFake(player)) continue;
            MessageTabProfiles.Entry entry = createEntry(player, getListName(player));
            String state = getState(player, entry);
            if (!state.equals(SENT_STATES.put(player.getUniqueID(), state))) {
                changed.put(player, entry);
            }
        }
        if (changed.isEmpty()) return;

        for (EntityPlayerMP recipient : players) {
            if (ServerUtils.isFake(recipient)) continue;
            List<MessageTabProfiles.Entry> visible = new ArrayList<>();
            for (Map.Entry<EntityPlayerMP, MessageTabProfiles.Entry> e : changed.entrySet()) {
                if (ServerUtils.canSee(recipient, e.getKey())) visible.add(e.getValue());
            }
            if (!visible.isEmpty()) {
                new MessageTabProfiles(visible).sendTo(recipient);
            }
        }
    }

    public static void remove(UUID id) {
        SENT_STATES.remove(id);
    }

    public static void clear() {
        SENT_STATES.clear();
        ticks = 0;
    }
}
