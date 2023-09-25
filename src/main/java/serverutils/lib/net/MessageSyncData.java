package serverutils.lib.net;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.lib.ServerUtilitiesLib;
import serverutils.lib.ServerUtilitiesLibCommon;
import serverutils.lib.ServerUtilitiesLibConfig;
import serverutils.lib.events.SyncGamerulesEvent;
import serverutils.lib.lib.client.ClientUtils;
import serverutils.lib.lib.data.ForgePlayer;
import serverutils.lib.lib.data.ISyncData;
import serverutils.lib.lib.io.Bits;
import serverutils.lib.lib.io.DataIn;
import serverutils.lib.lib.io.DataOut;
import serverutils.lib.lib.net.MessageToClient;
import serverutils.lib.lib.net.NetworkWrapper;
import serverutils.lib.lib.util.SidedUtils;
import serverutils.lib.lib.util.StringUtils;

public class MessageSyncData extends MessageToClient {

    private static final int LOGIN = 1;
    private static final int OP = 2;

    private int flags;
    private UUID universeId;
    private NBTTagCompound syncData;
    private Map<String, String> gamerules;

    public MessageSyncData() {}

    public MessageSyncData(boolean login, EntityPlayerMP player, ForgePlayer forgePlayer) {
        boolean op = MinecraftServer.getServer().getConfigurationManager().func_152596_g(player.getGameProfile());
        flags = Bits.setFlag(0, LOGIN, login);
        flags = Bits.setFlag(flags, OP, op);
        universeId = forgePlayer.team.universe.getUUID();
        syncData = new NBTTagCompound();

        for (Map.Entry<String, ISyncData> entry : ServerUtilitiesLibCommon.SYNCED_DATA.entrySet()) {
            syncData.setTag(entry.getKey(), entry.getValue().writeSyncData(player, forgePlayer));
        }

        gamerules = new HashMap<>();
        new SyncGamerulesEvent(
                gamerule -> gamerules.put(gamerule, player.worldObj.getGameRules().getGameRuleStringValue(gamerule)))
                        .post();
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerLibNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(flags);
        data.writeUUID(universeId);
        data.writeNBT(syncData);
        data.writeMap(gamerules, DataOut.STRING, DataOut.STRING);
    }

    @Override
    public void readData(DataIn data) {
        flags = data.readVarInt();
        universeId = data.readUUID();
        syncData = data.readNBT();
        gamerules = data.readMap(DataIn.STRING, DataIn.STRING);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        SidedUtils.UNIVERSE_UUID_CLIENT = universeId;

        for (String key : (Set<String>) syncData.func_150296_c()) {
            ISyncData nbt = ServerUtilitiesLibCommon.SYNCED_DATA.get(key);

            if (nbt != null) {
                nbt.readSyncData(syncData.getCompoundTag(key));
            }
        }

        for (Map.Entry<String, String> entry : gamerules.entrySet()) {
            Minecraft.getMinecraft().theWorld.getGameRules().setOrCreateGameRule(entry.getKey(), entry.getValue());
        }

        if (ServerUtilitiesLibConfig.debugging.print_more_info && Bits.getFlag(flags, LOGIN)) {
            ServerUtilitiesLib.LOGGER
                    .info("Synced data from universe " + StringUtils.fromUUID(SidedUtils.UNIVERSE_UUID_CLIENT));
        }

        ClientUtils.is_op = Bits.getFlag(flags, OP);
    }
}
