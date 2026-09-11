package serverutils.lib.util;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AtomicSaveTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void replacementPreservesPosixPermissions() throws Exception {
        Path target = temporary.newFile().toPath();
        org.junit.Assume.assumeTrue(Files.getFileStore(target).supportsFileAttributeView("posix"));
        Path reference = Files.createFile(target.resolveSibling("ordinary-new-file"));
        Path newTarget = target.resolveSibling("new-save.dat");
        FileUtils.writeAtomic(newTarget.toFile(), new byte[] { 1 });
        assertEquals(Files.getPosixFilePermissions(reference), Files.getPosixFilePermissions(newTarget));
        java.util.Set<java.nio.file.attribute.PosixFilePermission> permissions = java.nio.file.attribute.PosixFilePermissions
                .fromString("rw-rw----");
        Files.setPosixFilePermissions(target, permissions);
        FileUtils.writeAtomic(target.toFile(), new byte[] { 1 });
        assertEquals(permissions, Files.getPosixFilePermissions(target));
    }

    @Test
    public void failedSerializationPreservesPreviousSave() throws Exception {
        Path target = temporary.newFile().toPath();
        FileUtils.save(target.toFile(), "original");
        assertFalse(NBTUtils.writeNBTChecked(target.toFile(), null));
        assertEquals("original", new String(Files.readAllBytes(target), StandardCharsets.UTF_8));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("value", "replacement");
        assertTrue(NBTUtils.writeNBTChecked(target.toFile(), tag));
        assertEquals("replacement", NBTUtils.readNBT(target.toFile()).getString("value"));
    }

    @Test
    public void failedReplacementPreservesDestinationAndCleansTemporaryFile() throws Exception {
        Path directory = temporary.newFolder().toPath();
        Path target = Files.createDirectory(directory.resolve("target"));
        Files.write(target.resolve("keep"), new byte[] { 1 });
        try {
            FileUtils.writeAtomic(target.toFile(), new byte[] { 2 });
            fail("Expected replacement to fail");
        } catch (java.io.IOException expected) {}
        assertTrue(Files.isRegularFile(target.resolve("keep")));
        try (java.util.stream.Stream<Path> files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }
}
