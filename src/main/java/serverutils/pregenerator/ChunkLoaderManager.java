package serverutils.pregenerator;

import static com.gtnewhorizon.gtnhlib.util.CoordinatePacker.pack;
import static java.lang.System.nanoTime;
import static serverutils.ServerUtilitiesConfig.pregen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import net.minecraft.server.MinecraftServer;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import serverutils.ServerUtilities;
import serverutils.lib.util.misc.PregeneratorCommandInfo;
import serverutils.pregenerator.filemanager.PregeneratorFileManager;

public class ChunkLoaderManager {

    public final static ChunkLoaderManager instance = new ChunkLoaderManager();
    private final LongArrayFIFOQueue chunksToLoad = new LongArrayFIFOQueue(1000);
    private boolean isGenerating = false;
    private int dimensionID;
    private MinecraftServer serverType;
    private int totalChunksToLoad;
    private ChunkLoader loader;
    private PregeneratorFileManager fileManager;
    private int maxChunksToFind = Integer.MAX_VALUE;

    public void initializePregenerator(PregeneratorCommandInfo commandInfo, MinecraftServer server) throws IOException {
        findChunksToLoadCircle(commandInfo.getRadius(), commandInfo.getXLoc(), commandInfo.getZLoc());
        this.totalChunksToLoad = chunksToLoad.size();
        this.dimensionID = commandInfo.getDimensionID();
        this.isGenerating = true;
        this.serverType = server;
        this.fileManager = new PregeneratorFileManager(
                this.serverType,
                commandInfo.getXLoc(),
                commandInfo.getZLoc(),
                commandInfo.getRadius(),
                commandInfo.getDimensionID());
        this.loader = new ChunkLoader(this.fileManager);
    }

    public boolean initializeFromPregeneratorFiles(MinecraftServer server, int dimensionToCheck) {
        try {
            Path commandFolderPath = Paths.get("saves").resolve(server.getFolderName())
                    .resolve(PregeneratorFileManager.COMMAND_FOLDER);
            if (Files.exists(commandFolderPath.resolve(PregeneratorFileManager.COMMAND_FILE))
                    && Files.exists(commandFolderPath.resolve(PregeneratorFileManager.COMMAND_ITERATION))) {
                this.fileManager = new PregeneratorFileManager(server);
                Optional<PregeneratorCommandInfo> commandInfoOptional = this.fileManager.getCommandInfo();
                if (commandInfoOptional.isPresent()) {
                    PregeneratorCommandInfo commandInfo = commandInfoOptional.get();
                    if (commandInfo.getDimensionID() != dimensionToCheck) {
                        return false;
                    }
                    this.maxChunksToFind = commandInfo.getIteration();
                    findChunksToLoadCircle(commandInfo.getRadius(), commandInfo.getXLoc(), commandInfo.getZLoc());
                    this.totalChunksToLoad = chunksToLoad.size();
                    this.dimensionID = commandInfo.getDimensionID();
                    this.serverType = server;
                    this.isGenerating = true;
                    this.loader = new ChunkLoader(this.fileManager);
                    return this.fileManager.isReady();
                }
            }

        } catch (IOException e) {
            this.reset(true);
            e.printStackTrace();
        }
        return false;
    }

    public boolean isGenerating() {
        return this.isGenerating;
    }

    // Passed in xCenter and passed in zCenter are both in block coordinates. Be sure to transform to chunk coordinates
    // I've done a ton of testing with this. It works without duplicates and holes in the raster.
    public void findChunksToLoadCircle(int radius, double xCenter, double zCenter) {
        // This is a solved problem. I'll use the wikipedia entry on this:
        // https://en.wikipedia.org/wiki/Midpoint_circle_algorithm
        int chunkXCenter = (int) Math.floor(xCenter / 16);
        int chunkZCenter = (int) Math.floor(zCenter / 16);
        double decisionTracker = 1 - radius; // This is used to tell if we need to step X down.
        int x = radius;
        int z = 0;
        int previousX = radius;
        while (x >= z) {
            // Add all symmetrical points
            addChunk(chunkXCenter + x, chunkZCenter + z);
            addChunk(chunkXCenter - x, chunkZCenter + z);
            if (z != x) {
                addChunk(chunkXCenter + z, chunkZCenter + x);
                addChunk(chunkXCenter + z, chunkZCenter - x);
            }

            if (z != 0) {
                addChunk(chunkXCenter + x, chunkZCenter - z);
                addChunk(chunkXCenter - x, chunkZCenter - z);
                if (z != x) {
                    addChunk(chunkXCenter - z, chunkZCenter + x);
                    addChunk(chunkXCenter - z, chunkZCenter - x);
                }

            }

            if (x != previousX) {
                addChunksBetween(chunkXCenter + x, chunkZCenter - z, chunkZCenter + z);
                addChunksBetween(chunkXCenter - x, chunkZCenter - z, chunkZCenter + z);
            }
            previousX = x;

            if (x != z) {
                addChunksBetween(chunkXCenter + z, chunkZCenter - x, chunkZCenter + x);
                if (z != 0) {
                    addChunksBetween(chunkXCenter - z, chunkZCenter - x, chunkZCenter + x);
                }
            }

            if (chunksToLoad.size() >= maxChunksToFind) {
                break;
            }

            z++;
            if (decisionTracker < 0) {
                decisionTracker += 2 * z + 1;
            } else {
                x--;
                decisionTracker += 2 * (z - x) + 1;
            }
        }
        ServerUtilities.LOGGER.info("Found {} chunks to load", chunksToLoad.size());
    }

    public int getChunkToLoadSize() {
        return this.chunksToLoad.size();
    }

    public int getTotalChunksToLoad() {
        return this.totalChunksToLoad;
    }

    public int getDimensionID() {
        return dimensionID;
    }

    public void queueChunks(int numChunksToQueue) {
        final long startTime = nanoTime();
        for (int i = 0; i < numChunksToQueue; i++) {
            if (chunksToLoad.isEmpty()) {
                fileManager.closeAndRemoveAllFiles();
                isGenerating = false;
                break;
            } else if ((nanoTime() - startTime) >= pregen.timeLimitNanos()) break;

            loader.processLoadChunk(this.serverType, this.dimensionID, chunksToLoad.dequeueLastLong());
        }
    }

    public String progressString() {
        int chunksLoaded = totalChunksToLoad - chunksToLoad.size();
        double percentage = (double) chunksLoaded / totalChunksToLoad * 100;
        return String
                .format("Loaded %d chunks of a total of %d. %.1f%% done.", chunksLoaded, totalChunksToLoad, percentage);
    }

    public void reset(boolean hardReset) {
        this.isGenerating = false;
        this.chunksToLoad.clear();
        this.serverType = null;
        this.loader = null;
        this.dimensionID = Integer.MIN_VALUE;
        if (hardReset) {
            fileManager.closeAndRemoveAllFiles();
        } else {
            fileManager.closeAllFiles();
        }
        this.fileManager = null;
    }

    private void addChunk(int chunkX, int chunkZ) {
        if (chunksToLoad.size() >= maxChunksToFind) {
            return;
        }
        chunksToLoad.enqueue(pack(chunkX, 0, chunkZ));
    }

    private void addChunksBetween(int xLine, int zMin, int zMax) {
        for (int z = zMin + 1; z <= zMax - 1; z++) {
            addChunk(xLine, z);
        }
    }
}
