package serverutils.lib.net;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;

public abstract class MessageBase implements IMessage {

    MessageBase() {}

    public abstract NetworkWrapper getWrapper();

    @Override
    public final void toBytes(ByteBuf buf) {
        writeData(new DataOut(buf));
    }

    @Override
    public final void fromBytes(ByteBuf buf) {
        readData(new DataIn(buf));
    }

    public void writeData(DataOut data) {}

    public void readData(DataIn data) {}
}
