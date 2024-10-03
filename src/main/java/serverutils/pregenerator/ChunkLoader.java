package serverutils.pregenerator;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.gen.ChunkProviderServer;

import org.apache.commons.lang3.tuple.Pair;

import serverutils.ServerUtilities;
import serverutils.pregenerator.filemanager.PregeneratorFileManager;

public class ChunkLoader {

    private PregeneratorFileManager fileManager;
    private int loadIteration = 0;

    public ChunkLoader(PregeneratorFileManager fileManager) {
        this.fileManager = fileManager;
    }

    public void processLoadChunk(MinecraftServer server, int dimensionId, Pair<Integer, Integer> chunk) {
        int x = chunk.getLeft();
        int z = chunk.getRight();

        ChunkProviderServer cps = server.worldServerForDimension(dimensionId).theChunkProviderServer;
        cps.loadChunk(x, z, () -> {
            ChunkLoaderManager.instance.removeChunkFromList();
            this.fileManager.saveIteration(ChunkLoaderManager.instance.getChunkToLoadSize());
            loadIteration++;
            if (loadIteration % 100 == 0) {
                ServerUtilities.LOGGER.info(ChunkLoaderManager.instance.progressString());
            }
        });
    }
}
