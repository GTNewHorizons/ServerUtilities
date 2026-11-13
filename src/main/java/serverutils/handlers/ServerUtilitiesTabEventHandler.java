package serverutils.handlers;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.server.MinecraftServer;

import com.google.gson.JsonObject;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import serverutils.ServerUtilitiesConfig;
import serverutils.client.tab.TabChannelHandler;
import serverutils.lib.util.ServerUtils;
import serverutils.lib.util.TextVariables;

/**
 * Fills the variables of the tab list header and footer. Clients get the configured text through config sync and cannot
 * fill it themselves, so once a second the server sends everyone whose text changed the filled one, over the plugin
 * channel the client already takes a header and footer from.
 */
@EventBusSubscriber
public class ServerUtilitiesTabEventHandler {

    private static final int UPDATE_INTERVAL_TICKS = 20;

    /** What each player was sent last. */
    private static final Map<UUID, String> SENT = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = MinecraftServer.getServer();
        if (event.phase != TickEvent.Phase.END || server == null
                || server.getTickCounter() % UPDATE_INTERVAL_TICKS != 0) {
            return;
        }

        String header = ServerUtilitiesConfig.tab.headerText;
        String footer = ServerUtilitiesConfig.tab.footerText;

        if (!TextVariables.hasVariables(header) && !TextVariables.hasVariables(footer)) return;

        // The variables that are the same for everyone, once for all players.
        header = TextVariables.replace(header, server, null);
        footer = TextVariables.replace(footer, server, null);
        boolean perPlayer = TextVariables.hasPlayerVariables(header) || TextVariables.hasPlayerVariables(footer);

        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            if (ServerUtils.isFake(player) || player.playerNetServerHandler == null) continue;

            JsonObject json = new JsonObject();
            json.addProperty("header", perPlayer ? TextVariables.replace(header, server, player) : header);
            json.addProperty("footer", perPlayer ? TextVariables.replace(footer, server, player) : footer);
            String text = json.toString();

            if (!text.equals(SENT.put(player.getUniqueID(), text))) {
                player.playerNetServerHandler.sendPacket(
                        new S3FPacketCustomPayload(
                                TabChannelHandler.CHANNEL_NAME,
                                text.getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    // A player who comes back within the same second would otherwise be taken for up to date.
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.player.getUniqueID());
    }
}
