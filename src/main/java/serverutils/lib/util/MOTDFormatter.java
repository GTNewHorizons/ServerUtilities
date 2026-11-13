package serverutils.lib.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilitiesConfig;

public class MOTDFormatter {

    public static IChatComponent buildMOTD(MinecraftServer server) {
        if (!ServerUtilitiesConfig.motd.enabled) {
            return new ChatComponentText(server.getMOTD());
        }

        String line1 = TextVariables.replace(ServerUtilitiesConfig.motd.line1, server, null);
        String line2 = TextVariables.replace(ServerUtilitiesConfig.motd.line2, server, null);

        String combinedText = line1 + "\n" + line2;

        return new ChatComponentText(combinedText);
    }
}
