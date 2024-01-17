package serverutils.aurora.mc;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import serverutils.aurora.AuroraConfig;
import serverutils.aurora.PageType;
import serverutils.aurora.page.JsonWebPage;

public class PlayerListJson extends JsonWebPage {

    private final MinecraftServer server;

    public PlayerListJson(MinecraftServer s) {
        server = s;
    }

    @Override
    public PageType getPageType() {
        return switch (AuroraConfig.general.player_list_json) {
            case "DISABLED" -> PageType.DISABLED;
            case "REQUIRES_AUTH" -> PageType.REQUIRES_AUTH;
            default -> PageType.ENABLED;
        };
    }

    @Override
    public JsonElement getJson() {
        JsonObject json = new JsonObject();
        json.addProperty("max_players", server.getMaxPlayers());

        JsonArray players = new JsonArray();

        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            JsonObject o = new JsonObject();
            o.addProperty("name", player.getDisplayName());
            o.addProperty("uuid", player.getUniqueID().toString());
            players.add(o);
        }

        json.add("players", players);
        return json;
    }
}
