package serverutils.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import serverutils.ServerUtilities;
import serverutils.ServerUtilitiesConfig;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.data.ServerUtilitiesUniverseData;
import serverutils.lib.math.ChunkDimPos;
import serverutils.pregenerator.ChunkLoaderManager;

public class ServerUtilitiesWorldEventHandler {

    public static final ServerUtilitiesWorldEventHandler INST = new ServerUtilitiesWorldEventHandler();

    @SubscribeEvent
    public void onMobSpawned(EntityJoinWorldEvent event) {
        if (!event.world.isRemote && !isEntityAllowed(event.entity)) {
            event.entity.setDead();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDimensionUnload(WorldEvent.Unload event) {
        if (ClaimedChunks.isActive() && event.world.provider.dimensionId != 0) {
            ClaimedChunks.instance.markDirty();
        }
    }

    private static boolean isEntityAllowed(Entity entity) {
        if (entity instanceof EntityPlayer) {
            return true;
        }

        if (ServerUtilitiesConfig.world.safe_spawn
                && ServerUtilitiesUniverseData.isInSpawn(MinecraftServer.getServer(), new ChunkDimPos(entity))) {
            if (entity instanceof IMob) {
                return false;
            } else {
                return !(entity instanceof EntityChicken) || entity.riddenByEntity == null;
            }
        }

        return true;
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        World world = event.world;

        if (world.isRemote || event.getAffectedBlocks().isEmpty()) {
            return;
        }

        List<ChunkPosition> list = new ArrayList<>(event.getAffectedBlocks());
        event.getAffectedBlocks().clear();
        Map<ChunkDimPos, Boolean> map = new HashMap<>();
        final MinecraftServer server = MinecraftServer.getServer();

        Function<ChunkDimPos, Boolean> func = pos -> {
            if (pos.dim == 0 && ServerUtilitiesConfig.world.safe_spawn
                    && ServerUtilitiesUniverseData.isInSpawn(server, pos)) {
                return false;
            } else {
                if (ServerUtilitiesConfig.world.enable_explosions.isDefault()) {
                    ClaimedChunk chunk = ClaimedChunks.isActive() ? ClaimedChunks.instance.getChunk(pos) : null;
                    return chunk == null || chunk.hasExplosions();
                }

                return ServerUtilitiesConfig.world.enable_explosions.isTrue();
            }
        };

        for (ChunkPosition pos : list) {
            if (map.computeIfAbsent(
                    new ChunkDimPos(pos.chunkPosX, pos.chunkPosY, pos.chunkPosZ, world.provider.dimensionId),
                    func)) {
                event.getAffectedBlocks().add(pos);
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote) {
            int dimensionId = event.world.provider.dimensionId;
            MinecraftServer server = MinecraftServer.getServer();
            if (!ChunkLoaderManager.instance.isGenerating()
                    && ChunkLoaderManager.instance.initializeFromPregeneratorFiles(server, dimensionId)) {
                ServerUtilities.LOGGER.info("Pregenerator loaded and running for dimension Id: " + dimensionId);
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (!event.world.isRemote) {
            if (event.world.provider.dimensionId == ChunkLoaderManager.instance.getDimensionID()
                    && ChunkLoaderManager.instance.isGenerating()) {
                ChunkLoaderManager.instance.reset(false);
            }
        }
    }
}
