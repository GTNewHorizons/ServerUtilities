package serverutils.utils.net;

import java.util.Map;

import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.utils.gui.GuiLeaderboardList;

public class MessageLeaderboardListResponse extends MessageToClient {

    private Map<ResourceLocation, IChatComponent> leaderboards;

    public MessageLeaderboardListResponse() {}

    public MessageLeaderboardListResponse(Map<ResourceLocation, IChatComponent> l) {
        leaderboards = l;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.STATS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeMap(leaderboards, DataOut.RESOURCE_LOCATION, DataOut.TEXT_COMPONENT);
    }

    @Override
    public void readData(DataIn data) {
        leaderboards = data.readMap(DataIn.RESOURCE_LOCATION, DataIn.TEXT_COMPONENT);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new GuiLeaderboardList(leaderboards).openGui();
    }
}
