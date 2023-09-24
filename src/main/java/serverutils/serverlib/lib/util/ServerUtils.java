package serverutils.serverlib.lib.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.SpawnerAnimals; //WorldEntitySpawner
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import cpw.mods.fml.common.FMLCommonHandler;

import serverutils.serverlib.lib.math.BlockDimPos;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class ServerUtils
{
	public static final GameProfile FAKE_PLAYER_PROFILE = new GameProfile(StringUtils.fromString("069be1413c1b45c3b3b160d3f9fcd236"), "FakeForgePlayer");

	public enum SpawnType
	{
		CANT_SPAWN,
		ALWAYS_SPAWNS,
		ONLY_AT_NIGHT
	}

	public static double getMovementFactor(int dim)
	{
		switch (dim)
		{
			case 0:
			case 1:
				return 1D;
			case -1:
				return 8D;
			default:
			{
				WorldServer w = DimensionManager.getWorld(dim);
				return (w == null) ? 1D : w.provider.getMovementFactor();
			}
		}
	}

	public static IChatComponent getDimensionName(int dim)
	{
		switch (dim)
		{
			case 0:
				return new ChatComponentTranslation("createWorld.customize.preset.overworld");
			case -1:
				return new ChatComponentTranslation("achievements.nether.root.title");
			case 1:
				return new ChatComponentTranslation("achievements.end.root.title");
			default:
				for (DimensionType type : DimensionType.values())
				{
					if (type.getId() == dim)
					{
						return new ChatComponentText(type.getName());
					}
				}

				return new ChatComponentText("dim_" + dim);
		}
	}

	public static IChatComponent getDimensionName(DimensionType type)
	{
		switch (type)
		{
			case OVERWORLD:
				return new ChatComponentTranslation("createWorld.customize.preset.overworld");
			case NETHER:
				return new ChatComponentTranslation("advancements.nether.root.title");
			case THE_END:
				return new ChatComponentTranslation("advancements.end.root.title");
			default:
				return new ChatComponentText(type.getName());
		}
	}

	public static boolean isVanillaClient(ICommandSender sender)
	{
		if (sender instanceof EntityPlayerMP)
		{
			NetHandlerPlayServer connection = ((EntityPlayerMP) sender).playerNetServerHandler; //connection;
			return connection != null; // && !connection.netManager.channel().attr(NetworkRegistry.MOD_CONTAINER.).get();  //FML_MARKER).get(); //TODO
		}

		return false;
	}

	public static boolean isFake(EntityPlayerMP player)
	{
		return player.playerNetServerHandler == null || player instanceof FakePlayer;
	}

	public static boolean isOP(@Nullable MinecraftServer server, @Nullable GameProfile profile)
	{
		if (profile == null || profile.getId() == null || profile.getName() == null)
		{
			return false;
		}

		if (server == null)
		{
			server = FMLCommonHandler.instance().getMinecraftServerInstance();

			if (server == null)
			{
				return false;
			}
		}

		return server.getConfigurationManager().func_152596_g(profile);
			//getPlayerList().canSendCommands(profile);
	}

	public static boolean isOP(EntityPlayerMP player)
	{
		return isOP(player.mcServer, player.getGameProfile());
	}

	public static Collection<ICommand> getAllCommands(MinecraftServer server, ICommandSender sender)
	{
		Collection<ICommand> commands = new HashSet<>();
		for (Object c : server.getCommandManager().getPossibleCommands(sender))
		{
			ICommand command = (ICommand) c;
			if (command.canCommandSenderUseCommand(sender)) //checkPermission(server, sender))
			{
				commands.add(command);
			}
		}

		return commands;
	}

	public static SpawnType canMobSpawn(World world, BlockDimPos pos)
	{
		if (pos.posY < 0 || pos.posY >= 256)
		{
			return SpawnType.CANT_SPAWN;
		}

		Chunk chunk = world.getChunkFromChunkCoords(pos.posX, pos.posZ);

		//if (!SpawnerAnimals.canCreatureTypeSpawnAtLocation(EnumCreatureType.monster, world, pos.posY, pos.posX, pos.posZ) || chunk.getLightFor(EnumSkyBlock.Block, pos) >= 8)
		if (!SpawnerAnimals.canCreatureTypeSpawnAtLocation(EnumCreatureType.monster, world, pos.posY, pos.posX, pos.posZ) || chunk.getSavedLightValue(EnumSkyBlock.Block, pos.posX, pos.posY, pos.posZ) >= 8)
		{
			return SpawnType.CANT_SPAWN;
		}

		AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(pos.posX + 0.2, pos.posY + 0.01, pos.posZ + 0.2, pos.getBlockPos().posX + 0.8, pos.getBlockPos().posY + 1.8, pos.getBlockPos().posZ + 0.8);
		if (!world.checkNoEntityCollision(aabb) || world.isAnyLiquid(aabb))
		{
			return SpawnType.CANT_SPAWN;
		}

		return chunk.getSavedLightValue(EnumSkyBlock.Sky, pos.posX, pos.posY, pos.posZ) >= 8 ? SpawnType.ONLY_AT_NIGHT : SpawnType.ALWAYS_SPAWNS;
	}

	@Nullable
	public static Entity getEntityByUUID(World world, UUID uuid)
	{
		for (Object e : world.loadedEntityList)
		{
			Entity entity = (Entity) e;
			if (entity.getUniqueID().equals(uuid))
			{
				return entity;
			}
		}

		return null;
	}

	public static void notify(MinecraftServer server, @Nullable EntityPlayer player, IChatComponent component)
	{
		if (player == null)
		{
			for (Object p : server.getEntityWorld().playerEntities) //.getPlayerList().getPlayers())
			{
				EntityPlayer player1 = (EntityPlayer) p;
				player1.addChatMessage(component);
			}
		}
		else
		{
			player.addChatMessage(component);
		}
	}

	public static boolean isFirstLogin(EntityPlayer player, String key)
	{
		if (!NBTUtils.getPersistedData(player, false).getBoolean(key))
		{
			NBTUtils.getPersistedData(player, true).setBoolean(key, true);
			return true;
		}

		return false;
	}
}