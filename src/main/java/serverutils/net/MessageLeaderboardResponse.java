package serverutils.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.client.gui.GuiLeaderboard;
import serverutils.data.Leaderboard;
import serverutils.data.LeaderboardValue;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.Universe;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

public class MessageLeaderboardResponse extends MessageToClient {

    private static final DataOut.Serializer<LeaderboardValue> VALUE_SERIALIZER = (data, object) -> {
        data.writeString(object.username);
        data.writeTextComponent(object.value);
        data.writeByte(object.color.ordinal());
    };

    private static final DataIn.Deserializer<LeaderboardValue> VALUE_DESERIALIZER = data -> {
        LeaderboardValue value = new LeaderboardValue();
        value.username = data.readString();
        value.value = data.readTextComponent();
        value.color = EnumChatFormatting.values()[data.readUnsignedByte()];
        return value;
    };

    private IChatComponent title;
    private List<LeaderboardValue> values;

    public MessageLeaderboardResponse() {}

    public MessageLeaderboardResponse(EntityPlayerMP player, Leaderboard leaderboard) {
        title = leaderboard.getTitle();
        values = new ArrayList<>();

        ForgePlayer p0 = Universe.get().getPlayer(player);
        List<ForgePlayer> players = new ArrayList<>(Universe.get().getPlayers());
        players.sort(leaderboard.getComparator());

        for (int i = 0; i < players.size(); i++) {
            ForgePlayer p = players.get(i);
            LeaderboardValue value = new LeaderboardValue();
            value.username = p.getDisplayNameString();
            value.value = leaderboard.createValue(p);

            if (p == p0) {
                value.color = EnumChatFormatting.DARK_GREEN;
            } else if (!leaderboard.hasValidValue(p)) {
                value.color = EnumChatFormatting.DARK_GRAY;
            } else if (i < 3) {
                value.color = EnumChatFormatting.GOLD;
            } else {
                value.color = EnumChatFormatting.RESET;
            }

            values.add(value);
        }
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.STATS;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeTextComponent(title);
        data.writeCollection(values, VALUE_SERIALIZER);
    }

    @Override
    public void readData(DataIn data) {
        title = data.readTextComponent();
        values = new ArrayList<>();
        data.readCollection(values, VALUE_DESERIALIZER);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        new GuiLeaderboard(title, values).openGui();
    }
}
