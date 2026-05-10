package serverutils.lib.util;

import static serverutils.ServerUtilitiesConfig.commands;
import static serverutils.ServerUtilitiesConfig.teams;
import static serverutils.ServerUtilitiesConfig.world;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

public class SidedUtils {

    public static final Map<String, String> SERVER_MODS = new HashMap<>();

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
            case "serverutilities:trash_can" -> commands.trash_can;
            case "serverutilities:my_team" -> !teams.disable_teams;
            case "serverutilities:claimed_chunks" -> world.chunk_claiming;
            default -> true;
        };
    }
}
