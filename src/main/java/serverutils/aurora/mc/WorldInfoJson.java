package serverutils.aurora.mc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import serverutils.aurora.AuroraConfig;
import serverutils.aurora.PageType;
import serverutils.aurora.page.JsonWebPage;

public class WorldInfoJson extends JsonWebPage {

    private final MinecraftServer server;

    public WorldInfoJson(MinecraftServer s) {
        server = s;
    }

    @Override
    public PageType getPageType() {
        return AuroraConfig.pages.world_info_json;
    }

    @Override
    public JsonElement getJson() {

        WorldServer w = server.worldServerForDimension(0);
        JsonObject json = new JsonObject();

        json.addProperty("local_time", w.getWorldTime());
        json.addProperty("total_time", w.getTotalWorldTime());
        json.addProperty("daytime", w.isDaytime());
        json.addProperty("raining", w.isRaining());
        json.addProperty("thundering", w.isThundering());
        json.addProperty("moon_phase", w.getMoonPhase());

        return json;
    }
}
