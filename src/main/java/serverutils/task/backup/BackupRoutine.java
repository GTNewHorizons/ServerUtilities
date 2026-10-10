package serverutils.task.backup;

import com.github.bsideup.jabel.Desugar;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ThreadedFileIOBase;
import serverutils.ServerUtilities;
import serverutils.lib.OtherMods;
import serverutils.lib.data.Universe;
import serverutils.lib.util.compression.ICompress;

// clear separation of concern - routine is the routine (all the logic) for actually saving the world
// do not conflate logic
public class BackupRoutine {
    private final ICompress compressor;
    private final BackupOptions options;

    public BackupRoutine(ICompress compressor, BackupOptions options) {
        this.compressor = compressor;
        this.options = options;
    }

    public void save(Universe universe, Consumer<Boolean> callback) {
        preroutine(universe);

        Runnable work = () -> {
            try {
                doSave();
            } finally {
                cleanup(universe);
            }
        };

        if (options.useSeparateThread) {
            new Thread(work).start();
        } else {
            work.run();
        }
    }

    private void preroutine(Universe universe) {
        try {
            // Must run before saveAllChunks so level.dat is written with the current host inventory, otherwise
            // the single-player host's inventory in the backup is stale and items can dupe/vanish on restore.
            universe.server.getConfigurationManager().saveAllPlayerData();
            universe.disableWorldSaving();
            flushChunkSaves(universe.server.worldServers);
        } catch (MinecraftException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            cleanup(universe);
        }
    }

    private void doSave(Universe universe) {

    }

    private void cleanup(Universe universe) {
        universe.restoreWorldSaving();

    }

    private static void flushChunkSaves(WorldServer[] worlds) throws InterruptedException {
        // Let the worker finish first: concurrent writers can commit older chunk data last.
        ThreadedFileIOBase.threadedIOInstance.waitForFinish();
        for (WorldServer world : worlds) {
            if (world == null) continue;
            boolean suspended = world.levelSaving;
            try {
                world.levelSaving = false;
                // Vanilla can remove a loader just after new work is submitted to it. Drain that stranded work too.
                world.saveChunkData();
            } finally {
                world.levelSaving = suspended;
            }
        }
        ThreadedFileIOBase.threadedIOInstance.waitForFinish();
    }

    /** Reports retained Hodgepodge failures without requiring that mod, or its newer flush API. */
    private static void drainQueuedWrites() throws Exception {
        ThreadedFileIOBase.threadedIOInstance.waitForFinish();
        if (!OtherMods.isHodgepodgeLoaded()) return;
        Class<?> tweaks;
        try {
            tweaks = Class.forName("com.mitchej123.hodgepodge.config.TweaksConfig");
        } catch (ClassNotFoundException e) {
            return;
        }
        try {
            if (!tweaks.getField("threadedWorldDataSaving").getBoolean(null)) return;
        } catch (NoSuchFieldException e) {
            return;
        }
        flushWorldDataSaver(Class.forName("com.mitchej123.hodgepodge.util.WorldDataSaver"));
    }

    private static void flushWorldDataSaver(Class<?> saver) throws Exception {
        Method flush;
        try {
            flush = saver.getMethod("flush");
        } catch (NoSuchMethodException e) {
            ServerUtilities.LOGGER.warn(
                "Hodgepodge WorldDataSaver has no flush(); queued writes drained, but write failures cannot be confirmed");
            return;
        }
        try {
            flush.invoke(saver.getField("INSTANCE").get(null));
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException(cause);
        }
    }


    @Desugar
    public record BackupOptions(
        File world,
        boolean onlyClaimed,
        boolean incremental,
        boolean snapshotMetaFiles,
        boolean useSeparateThread
    ) {}
}
