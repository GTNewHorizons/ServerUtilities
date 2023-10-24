package serverutils.lib.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.SpawnerAnimals;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import serverutils.handlers.ServerUtilitiesClientEventHandler;
import serverutils.net.MessageNotification;

public class ServerUtils {

    public static final GameProfile FAKE_PLAYER_PROFILE = new GameProfile(
            StringUtils.fromString("069be1413c1b45c3b3b160d3f9fcd236"),
            "FakeForgePlayer");

    public enum SpawnType {
        CANT_SPAWN,
        ALWAYS_SPAWNS,
        ONLY_AT_NIGHT
    }

    public static double getMovementFactor(int dim) {
        return switch (dim) {
            case 0, 1 -> 1D;
            case -1 -> 8D;
            default -> {
                WorldServer w = DimensionManager.getWorld(dim);
                yield (w == null) ? 1D : w.provider.getMovementFactor();
            }
        };
    }

    public static IChatComponent getDimensionName(int dim) {
        return switch (dim) {
            case 0 -> new ChatComponentTranslation("serverutilities.world.dimension.overworld");
            case -1 -> new ChatComponentTranslation("serverutilities.world.dimension.nether");
            case 1 -> new ChatComponentTranslation("serverutilities.world.dimension.end");
            default -> new ChatComponentText("dim_" + dim);
        };
    }

    public static boolean isVanillaClient(ICommandSender sender) {
        if (sender instanceof EntityPlayerMP) {
            NetHandlerPlayServer connection = ((EntityPlayerMP) sender).playerNetServerHandler;
            return connection != null && connection.netManager.channel().attr(NetworkRegistry.MOD_CONTAINER) == null;
            // return connection != null; //&& !connection.netManager.channel().attr(NetworkRegistry.FML_MARKER).get();
        }

        return false;
    }

    public static boolean isFake(EntityPlayerMP player) {
        return player.playerNetServerHandler == null || player instanceof FakePlayer;
    }

    public static boolean isOP(@Nullable MinecraftServer server, @Nullable GameProfile profile) {
        if (profile == null || profile.getId() == null || profile.getName() == null) {
            return false;
        }

        if (server == null) {
            server = FMLCommonHandler.instance().getMinecraftServerInstance();

            if (server == null) {
                return false;
            }
        }

        return server.getConfigurationManager().func_152596_g(profile);
    }

    public static boolean isOP(EntityPlayerMP player) {
        return isOP(player.mcServer, player.getGameProfile());
    }

    public static Collection<ICommand> getAllCommands(MinecraftServer server, ICommandSender sender) {
        Collection<ICommand> commands = new HashSet<>();

        for (ICommand c : (Collection<ICommand>) server.getCommandManager().getCommands().values()) {
            if (c.canCommandSenderUseCommand(sender)) {
                commands.add(c);
            }
        }

        return commands;
    }

    public static SpawnType canMobSpawn(World world, int x, int y, int z) {
        if (y < 0 || y >= 256) {
            return SpawnType.CANT_SPAWN;
        }

        Chunk chunk = world.getChunkFromBlockCoords(x, z);

        boolean grounded_mob_spawn = SpawnerAnimals
                .canCreatureTypeSpawnAtLocation(EnumCreatureType.ambient, world, x, y, z)
                || SpawnerAnimals.canCreatureTypeSpawnAtLocation(EnumCreatureType.creature, world, x, y, z)
                || SpawnerAnimals.canCreatureTypeSpawnAtLocation(EnumCreatureType.monster, world, x, y, z);

        if (!grounded_mob_spawn || chunk.getSavedLightValue(EnumSkyBlock.Block, x, y, z) >= 8) {
            return SpawnType.CANT_SPAWN;
        }

        AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(x + 0.2, y + 0.01, z + 0.2, x + 0.8, y + 1.8, z + 0.8);
        if (!world.checkNoEntityCollision(aabb) || world.isAnyLiquid(aabb)) {
            return SpawnType.CANT_SPAWN;
        }

        return chunk.getSavedLightValue(EnumSkyBlock.Sky, x, y, z) >= 8 ? SpawnType.ONLY_AT_NIGHT
                : SpawnType.ALWAYS_SPAWNS;
    }

    @Nullable
    public static Entity getEntityByUUID(World world, UUID uuid) {
        for (Entity e : (List<Entity>) world.loadedEntityList) {
            if (e.getUniqueID().equals(uuid)) {
                return e;
            }
        }

        return null;
    }

    public static void notifyChat(MinecraftServer server, @Nullable EntityPlayer player, IChatComponent component) {
        if (player == null) {
            for (EntityPlayer player1 : (List<EntityPlayer>) server.getConfigurationManager().playerEntityList) {
                player1.addChatComponentMessage(component);
            }
        } else {
            player.addChatComponentMessage(component);
        }
    }

    public static void notify(MinecraftServer server, @Nullable EntityPlayer player, IChatComponent notification) {
        if (player == null) {
            new MessageNotification(notification).sendToAll();
        } else if (player instanceof EntityPlayerMP playerMP) {
            new MessageNotification(notification).sendTo(playerMP);
        } else if (player instanceof EntityClientPlayerMP) {
            ServerUtilitiesClientEventHandler.INST.onNotify(notification);
        }
    }

    public static boolean isFirstLogin(EntityPlayer player, String key) {
        if (!NBTUtils.getPersistedData(player, false).getBoolean(key)) {
            NBTUtils.getPersistedData(player, true).setBoolean(key, true);
            return true;
        }

        return false;
    }

    public static MinecraftServer getServer() {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    public static WorldServer getServerWorld() {
        MinecraftServer ms = getServer();
        return ms.worldServers[0];
    }
}
