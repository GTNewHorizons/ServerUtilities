package serverutils.utils.net;

import java.util.Map;

import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.net.MessageToClient;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import serverutils.utils.gui.GuiLeaderboardList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageLeaderboardListResponse extends MessageToClient {

    private Map<ResourceLocation, IChatComponent> leaderboards;

    public MessageLeaderboardListResponse() {}

    public MessageLeaderboardListResponse(Map<ResourceLocation, IChatComponent> l) {
        leaderboards = l;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return FTBUtilitiesNetHandler.STATS;
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
