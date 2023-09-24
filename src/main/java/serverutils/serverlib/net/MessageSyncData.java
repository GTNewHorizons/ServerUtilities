package serverutils.serverlib.net;

import serverutils.serverlib.ServerLib;
import serverutils.serverlib.ServerLibCommon;
import serverutils.serverlib.ServerLibConfig;
import serverutils.serverlib.events.SyncGamerulesEvent;
import serverutils.serverlib.lib.data.ForgePlayer;
import serverutils.serverlib.lib.data.ISyncData;
import serverutils.serverlib.lib.io.Bits;
import serverutils.serverlib.lib.io.DataIn;
import serverutils.serverlib.lib.io.DataOut;
import serverutils.serverlib.lib.net.MessageToClient;
import serverutils.serverlib.lib.net.NetworkWrapper;
import serverutils.serverlib.lib.util.SidedUtils;
import serverutils.serverlib.lib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageSyncData extends MessageToClient
{
	private static final int LOGIN = 1;

	private int flags;
	private UUID universeId;
	private NBTTagCompound syncData;
	private Map<String, String> gamerules;

	public MessageSyncData()
	{
	}

	public MessageSyncData(boolean login, EntityPlayerMP player, ForgePlayer forgePlayer)
	{
		flags = Bits.setFlag(0, LOGIN, login);
		universeId = forgePlayer.team.universe.getUUID();
		syncData = new NBTTagCompound();

		for (Map.Entry<String, ISyncData> entry : ServerLibCommon.SYNCED_DATA.entrySet())
		{
			syncData.setTag(entry.getKey(), entry.getValue().writeSyncData(player, forgePlayer));
		}

		gamerules = new HashMap<>();
		new SyncGamerulesEvent(gamerule -> gamerules.put(gamerule, player.worldObj.getGameRules().getGameRuleStringValue(gamerule))).post();
	}

	@Override
	public NetworkWrapper getWrapper()
	{
		return ServerLibNetHandler.GENERAL;
	}

	@Override
	public void writeData(DataOut data)
	{
		data.writeVarInt(flags);
		data.writeUUID(universeId);
		data.writeNBT(syncData);
		data.writeMap(gamerules, DataOut.STRING, DataOut.STRING);
	}

	@Override
	public void readData(DataIn data)
	{
		flags = data.readVarInt();
		universeId = data.readUUID();
		syncData = data.readNBT();
		gamerules = data.readMap(DataIn.STRING, DataIn.STRING);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void onMessage()
	{
		SidedUtils.UNIVERSE_UUID_CLIENT = universeId;

		for (Object k : syncData.func_150296_c())  //.getKeySet())
		{
			String key = (String) k;
			ISyncData nbt = ServerLibCommon.SYNCED_DATA.get(key);

			if (nbt != null)
			{
				nbt.readSyncData(syncData.getCompoundTag(key));
			}
		}

		for (Map.Entry<String, String> entry : gamerules.entrySet())
		{
			Minecraft.getMinecraft().theWorld.getGameRules().setOrCreateGameRule(entry.getKey(), entry.getValue());
		}

		if (ServerLibConfig.debugging.print_more_info && Bits.getFlag(flags, LOGIN))
		{
			ServerLib.LOGGER.info("Synced data from universe " + StringUtils.fromUUID(SidedUtils.UNIVERSE_UUID_CLIENT));
		}
	}
}