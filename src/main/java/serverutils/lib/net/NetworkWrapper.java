package serverutils.lib.net;

import java.util.Map;

import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleChannelHandlerWrapper;
import cpw.mods.fml.common.network.simpleimpl.SimpleIndexedCodec;
import cpw.mods.fml.relauncher.Side;

public class NetworkWrapper {

    private final SimpleIndexedCodec packetCodec;
    private final FMLEmbeddedChannel serverChannels;
    private final FMLEmbeddedChannel clientChannels;
    private int nextDiscriminator = 0;

    private NetworkWrapper(String s) {
        packetCodec = new SimpleIndexedCodec();
        Map<Side, FMLEmbeddedChannel> channels = NetworkRegistry.INSTANCE.newChannel(s, packetCodec);
        serverChannels = channels.get(Side.SERVER);
        clientChannels = channels.get(Side.CLIENT);
    }

    public static NetworkWrapper newWrapper(String id) {
        if (id.length() > 20) {
            throw new IllegalArgumentException("Network wrapper " + id + " id isn't valid, must be <= 20 characters!");
        }

        return new NetworkWrapper(id);
    }

    public FMLEmbeddedChannel getChannel(Side s) {
        return s.isServer() ? serverChannels : clientChannels;
    }

    public void registerBlank() {
        nextDiscriminator++;
    }

    public void register(MessageToClient m) {
        registerBlank();
        Class<? extends MessageToClient> clazz = m.getClass();
        packetCodec.addDiscriminator(nextDiscriminator, clazz);
        FMLEmbeddedChannel channel = getChannel(Side.CLIENT);
        String type = channel.findChannelHandlerNameForType(SimpleIndexedCodec.class);
        channel.pipeline().addAfter(
                type,
                clazz.getName(),
                new SimpleChannelHandlerWrapper<>(MessageToClientHandler.INSTANCE, Side.CLIENT, clazz));
    }

    public void register(MessageToServer m) {
        registerBlank();
        Class<? extends MessageToServer> clazz = m.getClass();
        packetCodec.addDiscriminator(nextDiscriminator, clazz);
        FMLEmbeddedChannel channel = getChannel(Side.SERVER);
        String type = channel.findChannelHandlerNameForType(SimpleIndexedCodec.class);
        channel.pipeline().addAfter(
                type,
                clazz.getName(),
                new SimpleChannelHandlerWrapper<>(MessageToServerHandler.INSTANCE, Side.SERVER, clazz));
    }
}
