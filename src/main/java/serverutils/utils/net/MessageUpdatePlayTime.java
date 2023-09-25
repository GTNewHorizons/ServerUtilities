package serverutils.utils.net;

import net.minecraft.client.Minecraft;
import net.minecraft.stats.StatList;

import com.feed_the_beast.ftblib.lib.io.DataIn;
import com.feed_the_beast.ftblib.lib.io.DataOut;
import com.feed_the_beast.ftblib.lib.net.MessageToClient;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageUpdatePlayTime extends MessageToClient {

    private int time;

    public MessageUpdatePlayTime() {}

    public MessageUpdatePlayTime(int t) {
        time = t;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return FTBUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(time);
    }

    @Override
    public void readData(DataIn data) {
        time = data.readVarInt();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        Minecraft.getMinecraft().thePlayer.getStatFileWriter()
                .func_150871_b(Minecraft.getMinecraft().thePlayer, StatList.minutesPlayedStat, time);
    }
}
