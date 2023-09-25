package serverutils.utils.net;

import net.minecraft.client.Minecraft;
import net.minecraft.stats.StatList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.lib.lib.net.NetworkWrapper;

public class MessageUpdatePlayTime extends MessageToClient {

    private int time;

    public MessageUpdatePlayTime() {}

    public MessageUpdatePlayTime(int t) {
        time = t;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
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
