package serverutils.pregenerator;

import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackX;
import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.unpackZ;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.gen.ChunkProviderServer;

import serverutils.ServerUtilities;
import serverutils.pregenerator.filemanager.PregeneratorFileManager;

public class ChunkLoader {

    private PregeneratorFileManager fileManager;
    private int loadIteration = 0;

    public ChunkLoader(PregeneratorFileManager fileManager) {
        this.fileManager = fileManager;
    }

    public void processLoadChunk(MinecraftServer server, int dimensionId, long chunk) {
        int x = unpackX(chunk);
        int z = unpackZ(chunk);

        ChunkProviderServer cps = server.worldServerForDimension(dimensionId).theChunkProviderServer;
        cps.loadChunk(x, z, () -> {
            this.fileManager.saveIteration(ChunkLoaderManager.instance.getChunkToLoadSize());
            loadIteration++;
            if (loadIteration % 100 == 0) {
                ServerUtilities.LOGGER.info(ChunkLoaderManager.instance.progressString());
            }
        });
    }
}
