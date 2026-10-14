package serverutils.lib.util;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.DataOutputStream;
import java.io.File;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import serverutils.lib.math.ChunkDimPos;

public class WorldRegionTest {

    private File root;

    @Before
    public void beforeEach() throws Exception {
        root = Files.createTempDirectory(new File("build").toPath(), "world-region-").toFile();
    }

    @After
    public void afterEach() {
        Thread.interrupted();
        FileUtils.delete(root);
    }

    @Test
    public void copyTo_WhenInterrupted_StopsBeforeDecodingChunks() throws Exception {
        File source = new File(root, "r.0.0.mca");
        File target = new File(root, "copy.mca");
        try (WorldRegion region = new WorldRegion(source, "overworld");
                DataOutputStream out = region.regionFile.getChunkDataOutputStream(0, 0)) {
            // An invalid NBT type, so decoding fails with a different exception if cancellation is checked too late.
            out.writeByte(99);
        }

        try (WorldRegion region = new WorldRegion(source, "overworld");
                WorldRegion copy = new WorldRegion(target, "overworld")) {
            Thread.currentThread().interrupt();
            assertThrows(
                    InterruptedIOException.class,
                    () -> region.copyTo(copy, Collections.singletonList(new ChunkDimPos(0, 0, 0))));
            assertTrue("Interrupt must stay set for the caller", Thread.currentThread().isInterrupted());
        }
    }
}
