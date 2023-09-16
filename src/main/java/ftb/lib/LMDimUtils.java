package ftb.lib;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.*;
import net.minecraftforge.common.DimensionManager;

public class LMDimUtils {

    public static boolean teleportPlayer(Entity entity, EntityPos pos) {
        return pos != null && teleportPlayer(entity, pos.x, pos.y, pos.z, pos.dim);
    }

    public static boolean teleportPlayer(Entity entity, BlockDimPos pos) {
        return pos != null && teleportPlayer(entity, pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D, pos.dim);
    }

    // tterrag's code, I don't own it. Maybe he doesn't too.. but who cares, eh?
    public static boolean teleportPlayer(Entity entity, double x, double y, double z, int dim) {
        if (entity == null) return false;
        entity.fallDistance = 0F;
        EntityPlayerMP player = entity instanceof EntityPlayer ? (EntityPlayerMP) entity : null;

        if (dim == entity.dimension) {
            if (x == entity.posX && y == entity.posY && z == entity.posZ) return true;

            if (player != null) {
                player.playerNetServerHandler.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
                return true;
            }
        }

        // FTBLib.dev_logger.info("Teleporting " + entity + " from " + new EntityPos(entity) + " to " + new EntityPos(x,
        // y, z, dim));
        int from = entity.dimension;
        float rotationYaw = entity.rotationYaw;
        float rotationPitch = entity.rotationPitch;
        MinecraftServer server = MinecraftServer.getServer();
        WorldServer fromDim = server.worldServerForDimension(from);
        WorldServer toDim = server.worldServerForDimension(dim);

        if (player != null) {
            server.getConfigurationManager().transferPlayerToDimension(player, dim, new TeleporterBlank(toDim));
            if (from == 1 && entity.isEntityAlive()) {
                // get around vanilla End hacks
                toDim.spawnEntityInWorld(entity);
                toDim.updateEntityWithOptionalForce(entity, false);
            }
        } else {
            NBTTagCompound tagCompound = new NBTTagCompound();
            entity.writeToNBT(tagCompound);
            Class<? extends Entity> entityClass = entity.getClass();
            fromDim.removeEntity(entity);

            try {
                Entity newEntity = entityClass.getConstructor(World.class).newInstance(toDim);
                newEntity.readFromNBT(tagCompound);
                newEntity.setLocationAndAngles(x, y, z, rotationYaw, rotationPitch);
                newEntity.forceSpawn = true;
                toDim.spawnEntityInWorld(newEntity);
                newEntity.forceSpawn = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        entity.fallDistance = 0;
        entity.rotationYaw = rotationYaw;
        entity.rotationPitch = rotationPitch;
        if (player != null) player.setPositionAndUpdate(x, y, z);
        else entity.setPosition(x, y, z);
        return true;

        /*
         * Ye olde teleport code MinecraftServer mcs = MinecraftServer.getServer(); if(mcs == null || (dim != 0 &&
         * !mcs.getAllowNether())) return false; WorldServer w1 = mcs.worldServerForDimension(dim); if(w1 == null) {
         * System.err.println("Cannot teleport " + ep.getCommandSenderName() + " to Dimension " + dim +
         * ": Missing WorldServer"); return false; } WorldServer w0 = (WorldServer) ep.worldObj; if(ep.ridingEntity !=
         * null) { ep.mountEntity(null); } boolean chw = w0 != w1; w0.updateEntityWithOptionalForce(ep, false);
         * ep.closeScreen(); if(chw) { ep.dimension = dim; ep.playerNetServerHandler.sendPacket(new
         * S07PacketRespawn(ep.dimension, ep.worldObj.difficultySetting, w1.getWorldInfo().getTerrainType(),
         * ep.theItemInWorldManager.getGameType())); w0.getPlayerManager().removePlayer(ep); ep.closeScreen();
         * w0.playerEntities.remove(ep); w0.updateAllPlayersSleepingFlag(); int i = ep.chunkCoordX; int j =
         * ep.chunkCoordZ; if(ep.addedToChunk && w0.getChunkProvider().chunkExists(i, j)) {
         * w0.getChunkFromChunkCoords(i, j).removeEntity(ep); w0.getChunkFromChunkCoords(i, j).isModified = true; }
         * w0.loadedEntityList.remove(ep); w0.onEntityRemoved(ep); } ep.setLocationAndAngles(x, y, z, ep.rotationYaw,
         * ep.rotationPitch); w1.theChunkProviderServer.loadChunk(MathHelper.floor_double(x) >> 4,
         * MathHelper.floor_double(z) >> 4); if(chw) { w1.spawnEntityInWorld(ep); ep.setWorld(w1); }
         * ep.setLocationAndAngles(x, y, z, ep.rotationYaw, ep.rotationPitch); w1.updateEntityWithOptionalForce(ep,
         * false); ep.setLocationAndAngles(x, y, z, ep.rotationYaw, ep.rotationPitch); if(chw)
         * ep.mcServer.getConfigurationManager().func_72375_a(ep, w1); ep.playerNetServerHandler.setPlayerLocation(x, y,
         * z, ep.rotationYaw, ep.rotationPitch); w1.updateEntityWithOptionalForce(ep, false);
         * ep.theItemInWorldManager.setWorld(w1);
         * ep.mcServer.getConfigurationManager().updateTimeAndWeatherForPlayer(ep, w1);
         * ep.mcServer.getConfigurationManager().syncPlayerInventory(ep); for(Object o : ep.getActivePotionEffects())
         * ep.playerNetServerHandler.sendPacket(new S1DPacketEntityEffect(ep.getEntityId(), (PotionEffect) o));
         * ep.playerNetServerHandler.sendPacket(new S1FPacketSetExperience(ep.experience, ep.experienceTotal,
         * ep.experienceLevel)); ep.setLocationAndAngles(x, y, z, ep.rotationYaw, ep.rotationPitch);
         */
    }

    public static World getWorld(int dim) {
        return DimensionManager.getWorld(dim);
    }

    public static String getDimName(int dim) {
        if (dim == 0) return "Overworld";
        else if (dim == 1) return "The End";
        else if (dim == -1) return "Nether";

        World w = getWorld(dim);
        return w == null ? ("DIM" + dim) : w.provider.getDimensionName();
    }

    public static double getMovementFactor(int dim) {
        if (dim == 0) return 1D;
        else if (dim == 1) return 1D;
        else if (dim == -1) return 8D;
        else {
            World w = getWorld(dim);
            return (w == null) ? 1D : w.provider.getMovementFactor();
        }
    }

    public static double getWorldScale(int dim) {
        return 1D / getMovementFactor(dim);
    }

    public static BlockDimPos getSpawnPoint(int dim) {
        World w = getWorld(dim);
        if (w == null) return null;
        ChunkCoordinates c = w.getSpawnPoint();
        if (c == null) return null;
        return new BlockDimPos(c, dim);
    }

    public static BlockDimPos getPlayerEntitySpawnPoint(EntityPlayerMP ep, int dim) {
        ChunkCoordinates c = ep.getBedLocation(dim);
        if (c == null) return getSpawnPoint(dim);
        return new BlockDimPos(c, dim);
    }

    private static class TeleporterBlank extends Teleporter {

        public TeleporterBlank(WorldServer w) {
            super(w);
        }

        public boolean makePortal(Entity e) {
            return true;
        }

        public boolean placeInExistingPortal(Entity e, double x, double y, double z, float f) {
            return true;
        }

        public void placeInPortal(Entity entity, double x, double y, double z, float f) {
            entity.setLocationAndAngles(x, y, z, entity.rotationPitch, entity.rotationYaw);
            entity.motionX = 0D;
            entity.motionY = 0D;
            entity.motionZ = 0D;
            entity.fallDistance = 0F;
        }

        public void removeStalePortalLocations(long l) {}
    }
}
