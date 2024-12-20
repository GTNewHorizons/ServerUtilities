package serverutils.lib.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

public class SidedUtils {

    public static Map<String, String> SERVER_MODS = new HashMap<>();
    public static UUID UNIVERSE_UUID_CLIENT = null;
    public static boolean trashCan = false, teams = false, chunkClaiming = false;

    public static IChatComponent lang(String key, Object... args) {
        return new ChatComponentTranslation(key, args);
    }

    /**
     * Checks from client side if a mod exists on server side
     */
    public static boolean isModLoadedOnServer(String modid) {
        return !modid.isEmpty() && SERVER_MODS.containsKey(modid);
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

    public static boolean isButtonEnabledOnServer(ResourceLocation buttonId) {
        return switch (buttonId.toString()) {
            case "serverutilities:trash_can" -> trashCan;
            case "serverutilities:my_team" -> teams;
            case "serverutilities:claimed_chunks" -> chunkClaiming;
            default -> true;
        };
    }
}
