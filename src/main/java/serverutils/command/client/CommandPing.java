package serverutils.command.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.INetHandlerStatusClient;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S00PacketServerInfo;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import serverutils.ServerUtilities;
import serverutils.lib.command.CmdBase;
import serverutils.lib.util.StringUtils;

public class CommandPing extends CmdBase {

    private final static Executor EXECUTOR = Executors.newScheduledThreadPool(1);

    public CommandPing() {
        super("ping", Level.ALL);
    }

    @Override
    public void processCommand(ICommandSender sender, String[] strings) throws CommandException {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getIntegratedServer() != null) {
            sender.addChatMessage(ServerUtilities.lang(sender, "commands.ping.integrated_server"));
            return;
        }
        ServerData data = mc.func_147104_D();
        if (data != null) {
            EXECUTOR.execute(() -> {
                ServerAddress address = ServerAddress.func_78860_a(data.serverIP);
                try {
                    NetworkManager networkManager = NetworkManager
                            .provideLanClient(InetAddress.getByName(address.getIP()), address.getPort());
                    networkManager.setNetHandler(new INetHandlerStatusClient() {

                        private long latency = -1L;
                        private long sendAt;

                        @Override
                        public void handleServerInfo(S00PacketServerInfo sPacketServerInfo) {
                            sendAt = Minecraft.getSystemTime();
                            networkManager.scheduleOutboundPacket(new C01PacketPing(sendAt));
                        }

                        @Override
                        public void handlePong(S01PacketPong sPacketPong) {
                            latency = Minecraft.getSystemTime() - sendAt;
                            networkManager.closeChannel(new ChatComponentText("Finished"));
                            sender.addChatMessage(
                                    ServerUtilities
                                            .lang(sender, "commands.ping.ping", StringUtils.getTimeString(latency)));
                        }

                        @Override
                        public void onDisconnect(IChatComponent component) {
                            if (latency == -1L) {
                                sender.addChatMessage(ServerUtilities.lang(sender, "commands.ping.unknown"));
                            }
                        }

                        @Override
                        public void onConnectionStateTransition(EnumConnectionState arg0, EnumConnectionState arg1) {
                            // TODO Auto-generated method stub
                        }

                        @Override
                        public void onNetworkTick() {
                            // TODO Auto-generated method stub
                        }
                    });
                    networkManager.scheduleOutboundPacket(
                            new C00Handshake(5, address.getIP(), address.getPort(), EnumConnectionState.STATUS));
                    networkManager.scheduleOutboundPacket(new C00PacketServerQuery());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    sender.addChatMessage(ServerUtilities.lang(sender, "commands.ping.unknown"));
                }
            });
            return;
        }
        sender.addChatMessage(ServerUtilities.lang(sender, "commands.ping.unknown"));
    }
}
