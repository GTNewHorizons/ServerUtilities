package serverutils.client.tab;

import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import io.netty.buffer.ByteBuf;
import serverutils.ServerUtilities;

public class TabChannelHandler {

    public static final String CHANNEL_NAME = "SU|TabHF";
    public static final TabChannelHandler INSTANCE = new TabChannelHandler();

    private volatile String header = "";
    private volatile String footer = "";
    private volatile boolean hasServerData = false;

    private TabChannelHandler() {}

    public void registerChannel() {
        FMLEventChannel channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(CHANNEL_NAME);
        channel.register(this);
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        try {
            ByteBuf payload = event.packet.payload();
            byte[] bytes = new byte[payload.readableBytes()];
            payload.readBytes(bytes);
            String json = new String(bytes, StandardCharsets.UTF_8);

            JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
            String h = obj.has("header") ? obj.get("header").getAsString() : "";
            String f = obj.has("footer") ? obj.get("footer").getAsString() : "";
            setServerData(h, f);
        } catch (Exception e) {
            ServerUtilities.LOGGER.warn("Failed to parse SU|TabHF packet", e);
        }
    }

    public void setServerData(String header, String footer) {
        this.header = header != null ? header : "";
        this.footer = footer != null ? footer : "";
        this.hasServerData = true;
    }

    public void clear() {
        header = "";
        footer = "";
        hasServerData = false;
    }

    public String getHeader() {
        return header;
    }

    public String getFooter() {
        return footer;
    }

    public boolean hasServerData() {
        return hasServerData;
    }
}
