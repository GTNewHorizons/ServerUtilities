package serverutils.lib.api.net;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import latmod.lib.ByteCount;
import latmod.lib.ByteIOStream;
import serverutils.lib.LMNBTUtils;

public abstract class MessageLM implements IMessage, IMessageHandler<MessageLM, IMessage> {

    public final NBTTagCompound readTag() {
        return LMNBTUtils.readTag(io);
    }

    public final void writeTag(NBTTagCompound tag) {
        LMNBTUtils.writeTag(io, tag);
    }

    // End of static //

    public final ByteCount dataType;
    public ByteIOStream io;

    public MessageLM(ByteCount t) {
        dataType = t;
        if (t != null) io = new ByteIOStream();
    }

    public abstract LMNetworkWrapper getWrapper();

    public IMessage onMessage(MessageContext ctx) {
        return null;
    }

    public final void fromBytes(ByteBuf bb) {
        if (dataType == null) return;

        int len = 0;

        if (dataType == ByteCount.BYTE) len = bb.readByte() & 0xFF;
        else if (dataType == ByteCount.SHORT) len = bb.readShort() & 0xFFFF;
        else if (dataType == ByteCount.INT) len = bb.readInt();

        byte[] b = new byte[len];
        bb.readBytes(b, 0, len);

        if (dataType == ByteCount.BYTE) io.setData(b);
        else io.setCompressedData(b);
    }

    public final void toBytes(ByteBuf bb) {
        if (dataType == null) return;

        byte[] b = (dataType == ByteCount.BYTE) ? io.toByteArray() : io.toCompressedByteArray();

        if (dataType == ByteCount.BYTE) bb.writeByte((byte) b.length);
        else if (dataType == ByteCount.SHORT) bb.writeShort((short) b.length);
        else if (dataType == ByteCount.INT) bb.writeInt(b.length);

        bb.writeBytes(b, 0, b.length);
    }

    public final IMessage onMessage(MessageLM m, MessageContext ctx) {
        io = m.io;
        return onMessage(ctx);
    }

    public final void sendTo(EntityPlayerMP ep) {
        // if(ServerUtilitiesLibraryFinals.DEV) ServerUtilitiesLibrary.logger.info("[S] Message sent: " +
        // getClass().getName());
        if (ep != null) getWrapper().sendTo(this, ep);
        else getWrapper().sendToAll(this);
    }

    public final void sendToServer() {
        // if(ServerUtilitiesLibraryFinals.DEV) ServerUtilitiesLibrary.logger.info("[C] Message sent: " +
        // getClass().getName());
        getWrapper().sendToServer(this);
    }
}
