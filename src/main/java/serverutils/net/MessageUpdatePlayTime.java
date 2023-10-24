package serverutils.net;

import net.minecraft.client.Minecraft;
import net.minecraft.stats.StatList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;

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
                .func_150873_a(Minecraft.getMinecraft().thePlayer, StatList.minutesPlayedStat, time);
    }
}
