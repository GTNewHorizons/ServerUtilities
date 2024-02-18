package serverutils.net;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesCommon;
import serverutils.ServerUtilitiesConfig;
import serverutils.events.SyncGamerulesEvent;
import serverutils.lib.client.ClientUtils;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ISyncData;
import serverutils.lib.io.Bits;
import serverutils.lib.io.DataIn;
import serverutils.lib.io.DataOut;
import serverutils.lib.net.MessageToClient;
import serverutils.lib.net.NetworkWrapper;
import serverutils.lib.util.SidedUtils;
import serverutils.lib.util.StringUtils;

public class MessageSyncData extends MessageToClient {

    private static final int LOGIN = 1;
    private static final int OP = 2;
    private static final int TRASH_CAN = 4;
    private static final int CHUNK_CLAIM = 8;
    private static final int TEAMS = 16;

    private int flags;
    private UUID universeId;
    private NBTTagCompound syncData;
    private Map<String, String> gamerules;
    private Map<String, String> modList = new HashMap<>();

    public MessageSyncData() {}

    public MessageSyncData(boolean login, EntityPlayerMP player, ForgePlayer forgePlayer) {
        boolean op = MinecraftServer.getServer().getConfigurationManager().func_152596_g(player.getGameProfile());
        boolean trash = ServerUtilitiesConfig.commands.trash_can;
        boolean claiming = ServerUtilitiesConfig.world.chunk_claiming;
        boolean teams = ServerUtilitiesConfig.teams.disable_teams;
        flags = Bits.setFlag(0, LOGIN, login);
        flags = Bits.setFlag(flags, OP, op);
        flags = Bits.setFlag(flags, TRASH_CAN, trash);
        flags = Bits.setFlag(flags, CHUNK_CLAIM, claiming);
        flags = Bits.setFlag(flags, TEAMS, !teams);
        universeId = forgePlayer.team.universe.getUUID();
        syncData = new NBTTagCompound();

        for (Map.Entry<String, ISyncData> entry : ServerUtilitiesCommon.SYNCED_DATA.entrySet()) {
            syncData.setTag(entry.getKey(), entry.getValue().writeSyncData(player, forgePlayer));
        }

        for (ModContainer container : Loader.instance().getActiveModList()) {
            modList.put(container.getModId(), container.getVersion());
        }

        gamerules = new HashMap<>();
        new SyncGamerulesEvent(
                gamerule -> gamerules.put(gamerule, player.worldObj.getGameRules().getGameRuleStringValue(gamerule)))
                        .post();
    }

    @Override
    public NetworkWrapper getWrapper() {
        return ServerUtilitiesNetHandler.GENERAL;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(flags);
        data.writeUUID(universeId);
        data.writeNBT(syncData);
        data.writeMap(gamerules, DataOut.STRING, DataOut.STRING);
        data.writeMap(modList, DataOut.STRING, DataOut.STRING);
    }

    @Override
    public void readData(DataIn data) {
        flags = data.readVarInt();
        universeId = data.readUUID();
        syncData = data.readNBT();
        gamerules = data.readMap(DataIn.STRING, DataIn.STRING);
        modList = data.readMap(DataIn.STRING, DataIn.STRING);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        SidedUtils.UNIVERSE_UUID_CLIENT = universeId;

        for (String key : syncData.func_150296_c()) {
            ISyncData nbt = ServerUtilitiesCommon.SYNCED_DATA.get(key);

            if (nbt != null) {
                nbt.readSyncData(syncData.getCompoundTag(key));
            }
        }

        for (Map.Entry<String, String> entry : gamerules.entrySet()) {
            Minecraft.getMinecraft().theWorld.getGameRules().setOrCreateGameRule(entry.getKey(), entry.getValue());
        }

        if (ServerUtilitiesConfig.debugging.print_more_info && Bits.getFlag(flags, LOGIN)) {
            ServerUtilities.LOGGER
                    .info("Synced data from universe " + StringUtils.fromUUID(SidedUtils.UNIVERSE_UUID_CLIENT));
        }
        ClientUtils.is_op = Bits.getFlag(flags, OP);

        SidedUtils.SERVER_MODS.putAll(modList);
        SidedUtils.trashCan = Bits.getFlag(flags, TRASH_CAN);
        SidedUtils.chunkClaiming = Bits.getFlag(flags, CHUNK_CLAIM);
        SidedUtils.teams = Bits.getFlag(flags, TEAMS);
    }
}
