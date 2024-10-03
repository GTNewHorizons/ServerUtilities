package serverutils.pregenerator;

import net.minecraft.server.MinecraftServer;
import org.apache.commons.lang3.tuple.Pair;
import serverutils.lib.util.misc.PregeneratorCommandInfo;
import serverutils.pregenerator.filemanager.PregeneratorFileManager;

import java.io.IOException;
import java.util.Optional;
import java.util.Vector;

public class ChunkLoaderManager
{
    public final static ChunkLoaderManager instance = new ChunkLoaderManager();
    private boolean isGenerating = false;
    private int dimensionID;
    private MinecraftServer serverType;
    private Vector<Pair<Integer, Integer>> chunksToLoad = new Vector<>(1000);
    private int totalChunksToLoad;
    private int chunkToLoadIndex;
    private ChunkLoader loader;
    private PregeneratorFileManager fileManager;

    public void initializePregenerator(PregeneratorCommandInfo commandInfo, MinecraftServer server) throws IOException {
        findChunksToLoadCircle(commandInfo.getRadius(), commandInfo.getXLoc(), commandInfo.getZLoc());
        this.totalChunksToLoad = chunksToLoad.size();
        this.chunkToLoadIndex = chunksToLoad.size() - 1;
        this.dimensionID = commandInfo.getDimensionID();
        this.isGenerating = true;
        this.serverType = server;
        this.fileManager = new PregeneratorFileManager(this.serverType, commandInfo.getXLoc(), commandInfo.getZLoc(), commandInfo.getRadius(), commandInfo.getDimensionID());
        this.loader = new ChunkLoader(this.fileManager);
    }

    public boolean initializeFromPregeneratorFiles(MinecraftServer server, int dimensionToCheck)
    {
        try
        {
            this.fileManager = new PregeneratorFileManager(server);
            Optional<PregeneratorCommandInfo> commandInfoOptional = this.fileManager.getCommandInfo();
            if (commandInfoOptional.isPresent())
            {
                PregeneratorCommandInfo commandInfo = commandInfoOptional.get();
                if (commandInfo.getDimensionID() != dimensionToCheck)
                {
                    return false;
                }
                findChunksToLoadCircle(commandInfo.getRadius(), commandInfo.getXLoc(), commandInfo.getZLoc());
                this.totalChunksToLoad = chunksToLoad.size();
                this.chunkToLoadIndex = commandInfo.getIteration() - 1;
                this.dimensionID = commandInfo.getDimensionID();
                if (this.chunkToLoadIndex < chunksToLoad.size()) {
                    this.chunksToLoad.subList(this.chunkToLoadIndex + 1, chunksToLoad.size()).clear();
                    this.serverType = server;
                    this.isGenerating = true;
                    this.loader = new ChunkLoader(this.fileManager);
                    return this.fileManager.isReady();
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isGenerating()
    {
        return this.isGenerating;
    }

    // Passed in xCenter and passed in zCenter are both in block coordinates. Be sure to transform to chunk coordinates
    // I've done a ton of testing with this. It works without duplicates and holes in the raster.
    public void findChunksToLoadCircle(int radius, double xCenter, double zCenter)
    {
        // This is a solved problem. I'll use the wikipedia entry on this: https://en.wikipedia.org/wiki/Midpoint_circle_algorithm
        int chunkXCenter = (int) Math.floor(xCenter / 16);
        int chunkZCenter = (int) Math.floor(zCenter / 16);
        double decisionTracker = 1 - radius; // This is used to tell if we need to step X down.
        int x = radius;
        int z = 0;
        int previousX = radius;
        while (x >= z)
        {
            // Add all symmetrical points
            addChunk(chunkXCenter + x, chunkZCenter + z);
            addChunk(chunkXCenter - x, chunkZCenter + z);
            if (z != x)
            {
                addChunk(chunkXCenter + z, chunkZCenter + x);
                addChunk(chunkXCenter + z, chunkZCenter - x);
            }


            if (z != 0)
            {
                addChunk(chunkXCenter + x, chunkZCenter - z);
                addChunk(chunkXCenter - x, chunkZCenter - z);
                if (z != x)
                {
                    addChunk(chunkXCenter - z, chunkZCenter + x);
                    addChunk(chunkXCenter - z, chunkZCenter - x);
                }

            }

            if(x != previousX)
            {
                addChunksBetween(chunkXCenter + x, chunkZCenter - z, chunkZCenter + z);
                addChunksBetween(chunkXCenter - x, chunkZCenter - z, chunkZCenter + z);
            }
            previousX = x;

            if (x != z)
            {
                addChunksBetween( chunkXCenter + z, chunkZCenter - x, chunkZCenter + x);
                if (z != 0)
                {
                    addChunksBetween(chunkXCenter - z, chunkZCenter - x, chunkZCenter + x);
                }
            }

            z++;
            if (decisionTracker < 0)
            {
                decisionTracker += 2 * z + 1;
            }
            else
            {
                x--;
                decisionTracker += 2 * (z - x) + 1;
            }
        }
        System.out.printf("Found %s chunks to load", chunksToLoad.size());
    }

    public void removeChunkFromList()
    {
        this.chunksToLoad.remove(this.chunksToLoad.size() - 1);
    }

    public int getChunkToLoadSize()
    {
        return this.chunksToLoad.size();
    }

    public int getTotalChunksToLoad()
    {
        return this.totalChunksToLoad;
    }

    public int getDimensionID()
    {
        return dimensionID;
    }
    public void queueChunks(int numChunksToQueue)
    {
        for (int i = 0; i < numChunksToQueue; i++)
        {
            if (!chunksToLoad.isEmpty())
            {
                loader.processLoadChunk(this.serverType, this.dimensionID, chunksToLoad.get(chunkToLoadIndex));
                chunkToLoadIndex--;
            }
            else
            {
                fileManager.closeAndRemoveAllFiles();
                isGenerating = false;
            }
        }
    }
    public String progressString()
    {
        int chunksLoaded = totalChunksToLoad - chunksToLoad.size();;
        double percentage = (double) chunksLoaded / totalChunksToLoad * 100;
        return String.format("Loaded %d chunks of a total of %d. %.1f%% done.", chunksLoaded, totalChunksToLoad, percentage);
    }

    public void reset(boolean hardReset)
    {
        this.isGenerating = false;
        this.chunksToLoad.clear();
        this.chunkToLoadIndex = -1;
        this.serverType = null;
        this.loader = null;
        this.dimensionID = Integer.MIN_VALUE;
        if (hardReset)
        {
            fileManager.closeAndRemoveAllFiles();
        }
        else
        {
            fileManager.closeAllFiles();
        }
        this.fileManager = null;
    }

    private void addChunk(int chunkX, int chunkZ)
    {
        chunksToLoad.add(Pair.of(chunkX, chunkZ));
    }

    private void addChunksBetween(int xLine, int zMin, int zMax)
    {
        for (int z = zMin + 1; z <= zMax - 1; z++) {
            addChunk(xLine, z);
        }
    }
}
