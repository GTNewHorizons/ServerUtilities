package serverutils.lib.lib.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;

public class SidedUtils {

    private static final Map<String, String> SERVER_MODS_0 = new HashMap<>();
    public static final Map<String, String> SERVER_MODS = Collections.unmodifiableMap(SERVER_MODS_0);

    public static UUID UNIVERSE_UUID_CLIENT = null;

    public static IChatComponent lang(@Nullable ICommandSender sender, String mod, String key, Object... args) {
        if (ServerUtils.isVanillaClient(sender)) {
            return new ChatComponentText(I18n.format(key, args));
        }
        return new ChatComponentTranslation(key, args);
    }

    public static void checkModLists(@Nullable Side side, @Nullable Map<String, String> map) {
        if (side == Side.SERVER) {
            SERVER_MODS_0.clear();

            if (map != null && !map.isEmpty()) {
                SERVER_MODS_0.putAll(map);
            }
        } else if (side == Side.CLIENT) {}
    }

    /**
     * Checks from client side if a mod exists on server side
     */
    public static boolean isModLoadedOnServer(String modid) {
        return !modid.isEmpty() && SERVER_MODS_0.containsKey(modid);
    }

    /**
     * Checks from client side if a set of mods exists on server side
     */
    public static boolean areAllModsLoadedOnServer(Collection<String> modids) {
        if (!modids.isEmpty()) {
            for (String modid : modids) {
                if (!isModLoadedOnServer(modid)) {
                    return false;
                }
            }
        }

        return true;
    }
}
