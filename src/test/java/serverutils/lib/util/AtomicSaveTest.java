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
    public void savesThroughFileLinksPreserveLinksAndUpdateTheirDestination() throws Exception {
        for (boolean relative : new boolean[] { false, true }) {
            Path root = temporary.newFolder().toPath();
            Path storage = Files.createDirectory(root.resolve("storage"));
            Path links = Files.createDirectory(root.resolve("links"));
            Path target = Files.write(storage.resolve("shared.dat"), new byte[] { 1 });
            Path link = links.resolve("save.dat");
            Path destination = relative ? links.relativize(target) : target.toAbsolutePath();
            createSymbolicLink(link, destination);
            Path alias = root.resolve("alias.dat");
            createSymbolicLink(alias, root.relativize(link));

            FileUtils.writeAtomic(link.toFile(), new byte[] { 2 });
            assertArrayEquals(new byte[] { 2 }, Files.readAllBytes(target));
            FileUtils.writeAtomic(alias.toFile(), new byte[] { 3 });
            assertArrayEquals(new byte[] { 3 }, Files.readAllBytes(target));
            assertEquals(destination, Files.readSymbolicLink(link));
            assertEquals(root.relativize(link), Files.readSymbolicLink(alias));
            assertTrue(Files.isSameFile(alias, target));

            assertThrows(NullPointerException.class, () -> FileUtils.writeAtomic(alias.toFile(), null));
            assertArrayEquals(new byte[] { 3 }, Files.readAllBytes(target));
            assertTrue(Files.isSymbolicLink(link));
            assertTrue(Files.isSymbolicLink(alias));
            try (java.util.stream.Stream<Path> files = Files.walk(root)) {
                assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".su-save-")));
            }
        }
    }

    @Test
    public void danglingAndCyclicFileLinksFailWithoutReplacingTheLinks() throws Exception {
        Path root = temporary.newFolder().toPath();
        Path missing = root.resolve("missing.dat");
        Path link = root.resolve("save.dat");
        createSymbolicLink(link, missing.getFileName());
        assertThrows(
                java.nio.file.NoSuchFileException.class,
                () -> FileUtils.writeAtomic(link.toFile(), new byte[] { 1 }));
        assertEquals(missing.getFileName(), Files.readSymbolicLink(link));
        assertFalse(Files.exists(missing));
        // Turn the dangling destination into a cycle, without changing the original link.
        createSymbolicLink(missing, link.getFileName());
        assertThrows(java.io.IOException.class, () -> FileUtils.writeAtomic(link.toFile(), new byte[] { 2 }));
        assertEquals(missing.getFileName(), Files.readSymbolicLink(link));
        assertEquals(link.getFileName(), Files.readSymbolicLink(missing));
        try (java.util.stream.Stream<Path> files = Files.list(root)) {
            assertEquals(2, files.count());
        }
    }

    private static void createSymbolicLink(Path link, Path target) throws Exception {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException unavailable) {
            org.junit.Assume.assumeNoException("File symlinks unavailable", unavailable);
        }
    }

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

        java.util.Set<java.nio.file.attribute.PosixFilePermission> readOnlyOwner = java.nio.file.attribute.PosixFilePermissions
                .fromString("r--rw-rw-");
        Files.setPosixFilePermissions(target, readOnlyOwner);
        FileUtils.writeAtomic(target.toFile(), new byte[] { 2 });
        assertEquals(readOnlyOwner, Files.getPosixFilePermissions(target));
        assertArrayEquals(new byte[] { 2 }, Files.readAllBytes(target));
    }

    @Test
    public void replacementStagingStartsPrivateAndWritable() throws Exception {
        Path target = temporary.newFile().toPath();
        org.junit.Assume.assumeTrue(Files.getFileStore(target).supportsFileAttributeView("posix"));
        Files.setPosixFilePermissions(target, java.nio.file.attribute.PosixFilePermissions.fromString("r--------"));
        Path staged = FileUtils.createSaveTemporary(target);
        try {
            assertEquals(
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"),
                    Files.getPosixFilePermissions(staged));
            Files.write(staged, new byte[] { 1 });
            assertArrayEquals(new byte[] { 1 }, Files.readAllBytes(staged));
        } finally {
            Files.deleteIfExists(staged);
        }
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
