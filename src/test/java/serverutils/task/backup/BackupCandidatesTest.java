package serverutils.task.backup;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import serverutils.lib.util.FileUtils;

public class BackupCandidatesTest extends BackupFixtures.Tests {

    @Before
    public void beforeEach() {
        testSetup(SetupFlags.FOLDERS);
    }

    @After
    public void afterEach() {
        testReset();
    }

    @Test
    public void scan_WhenWorldIsInBackupStorage_ShouldThrowException() {
        IOException exception = assertThrows(IOException.class, () -> BackupCandidates.scanWorld(world, saves, false));
        assertEquals(
                "Backup and temporary storage must not contain the world directory: " + world,
                exception.getMessage());
    }

    @Test
    public void scan_WhenWorldDoesntExist_ShouldThrowException() {
        File someDir = new File(build, UUID.randomUUID().toString());
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BackupCandidates.scanWorld(someDir, backups, false));
        assertEquals("World does not exist: " + someDir, exception.getMessage());
    }

    @Test
    public void scan_WhenWorldIsInvalid_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> BackupCandidates.scanWorld(saves, backups, false));
        assertEquals("World is invalid: " + saves, exception.getMessage());
    }

    @Test
    public void scan_WhenBackupStorageInsideWorld_SkipIt() throws IOException {
        File nestedBackups = new File(world, "backups");
        File archived = new File(nestedBackups, "deep/old.zip");
        File keep = new File(world, "keep.dat");
        Files.createDirectories(archived.getParentFile().toPath());
        Files.write(archived.toPath(), new byte[] { 1 });
        Files.write(keep.toPath(), new byte[] { 2 });

        Set<File> files = files(BackupCandidates.scanWorld(world, nestedBackups, false));

        assertTrue(files.contains(keep));
        assertFalse(files.contains(archived));
    }

    @Test
    public void scan_WhenFileLinksIntoBackupStorage_SkipUt() throws IOException {
        File target = new File(backups, "target.zip");
        File link = new File(world, "linked-backup.zip");
        Files.write(target.toPath(), new byte[] { 1 });
        try {
            Files.createSymbolicLink(link.toPath(), target.toPath().toAbsolutePath());
        } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
            Assume.assumeNoException(unsupported);
        }

        Set<File> files = files(BackupCandidates.scanWorld(world, backups, false));

        assertFalse(files.contains(link));
    }

    @Test
    public void scan_WhenDirectoryLinked_FollowsOrdinaryAndSkipsBackupStorage() throws IOException {
        File outside = FileUtils.ensureExists(new File(build, "outside"));
        File kept = new File(outside, "keep.dat");
        File archived = new File(backups, "old.zip");
        Files.write(kept.toPath(), new byte[] { 1 });
        Files.write(archived.toPath(), new byte[] { 2 });

        File linked = new File(world, "linked");
        File excluded = new File(world, "excluded");
        try {
            Files.createSymbolicLink(linked.toPath(), outside.toPath().toAbsolutePath());
            Files.createSymbolicLink(excluded.toPath(), backups.toPath().toAbsolutePath());
        } catch (IOException | UnsupportedOperationException | SecurityException unsupported) {
            Assume.assumeNoException(unsupported);
        }

        Set<File> files = files(BackupCandidates.scanWorld(world, backups, false));

        assertTrue(files.contains(new File(linked, "keep.dat")));
        assertFalse(files.contains(new File(excluded, "old.zip")));
    }

    @Test
    public void removeMatching_SupportsLiteralGlobAbsoluteAndWorldNamePatterns() throws IOException {
        File literal = write(world, "literal.dat");
        File similar = write(world, "literal.dat.bak");
        File belowLiteral = write(world, "literal-dir/nested/data.dat");
        File belowPlain = write(world, "plain-dir/data.dat");
        File topLevelGlob = write(world, "top.tmp");
        File belowSingleStar = write(world, "nested/nested.tmp");
        File belowDoubleStar = write(world, "cache/a/b/cached.dat");
        File belowWorldName = write(world, "named/deep/named.dat");
        File absolute = write(world, "absolute.dat");
        File kept = write(world, "keep.dat");
        File characterClass = write(world, "class1.cfg");
        File alternative = write(world, "choiceB.dat");
        File singleCharacter = write(world, "questionA.txt");
        File parentSegment = write(world, "data/parent-normalized.dat");
        File repeatedSeparator = write(world, "data/double-separator.dat");

        String worldPath = FileUtils.getRelativePath(world);
        String savesPath = FileUtils.getRelativePath(saves);
        BackupCandidates candidates = BackupCandidates.scanWorld(world, backups, false);
        candidates.removeMatching(
                Arrays.asList(
                        "",
                        worldPath + "/literal.dat",
                        worldPath + "/literal-dir/**",
                        worldPath + "/plain-dir",
                        worldPath + "/*.tmp",
                        worldPath + "/cache/**",
                        savesPath.replace('/', '\\') + "/$WORLDNAME/named/**",
                        absolute.getAbsolutePath(),
                        worldPath + "/class[12].cfg",
                        worldPath + "/choice{A,B}.dat",
                        worldPath + "/question?.txt",
                        worldPath + "/data/../data/parent-normalized.dat",
                        worldPath + "//data/double-separator.dat"),
                world.getName());

        Set<File> files = files(candidates);
        for (File removed : new File[] { literal, belowLiteral, topLevelGlob, belowDoubleStar, belowWorldName, absolute,
                characterClass, alternative, singleCharacter, parentSegment, repeatedSeparator }) {
            assertFalse(removed.getPath(), files.contains(removed));
        }
        // a literal folder needs /** to match its contents, and * does not cross folders
        for (File remaining : new File[] { similar, belowPlain, belowSingleStar, kept }) {
            assertTrue(remaining.getPath(), files.contains(remaining));
        }
    }

    @Test
    public void removeMatching_OverridesAdditionalFilesButKeepsRegions() throws IOException {
        File region = write(world, "region/r.0.0.mca");
        File excluded = write(build, "additional/excluded.cfg");
        File included = write(build, "additional/included.cfg");

        BackupCandidates candidates = BackupCandidates.scanWorld(world, backups, false);
        candidates.addFiles(FileUtils.findMatching(FileUtils.getRelativePath(new File(build, "additional")) + "/**"));
        candidates.removeMatching(
                Arrays.asList(FileUtils.getRelativePath(excluded), FileUtils.getRelativePath(world) + "/**"),
                world.getName());

        Set<File> files = absolute(candidates.getFiles().stream().map(WorldFile::backing).collect(Collectors.toList()));
        assertFalse(files.contains(excluded.getAbsoluteFile()));
        assertTrue(files.contains(included.getAbsoluteFile()));
        assertTrue(files.contains(region.getAbsoluteFile()));
    }

    @Test
    public void removeMatching_ExcludesMcaOutsideRegionFolders() throws IOException {
        File nested = write(world, "region/unowned/r.0.0.mca");
        File loose = write(world, "r.0.0.mca");

        BackupCandidates candidates = BackupCandidates.scanWorld(world, backups, false);
        candidates.removeMatching(Collections.singletonList(FileUtils.getRelativePath(world) + "/**"), world.getName());

        Set<File> files = files(candidates);
        assertFalse(files.contains(nested));
        assertFalse(files.contains(loose));
    }

    @Test
    public void removeMatching_NeverExcludesLevelDat() throws IOException {
        File levelDat = new File(world, "level.dat");
        File other = write(world, "other.dat");

        for (String pattern : new String[] { FileUtils.getRelativePath(levelDat),
                FileUtils.getRelativePath(world) + "/**", FileUtils.getRelativePath(saves) + "/$WORLDNAME/level.dat",
                levelDat.getAbsolutePath() }) {
            BackupCandidates candidates = BackupCandidates.scanWorld(world, backups, false);
            candidates.removeMatching(Collections.singletonList(pattern), world.getName());

            Set<File> files = files(candidates);
            assertTrue(pattern, files.contains(levelDat));
            if (pattern.endsWith("/**")) assertFalse(pattern, files.contains(other));
        }
    }

    @Test
    public void findMatching_SelectsOnlyMatchingFiles() throws IOException {
        File selected = write(build, "config/server.cfg");
        File ignored = write(build, "config/private.cfg/hidden.cfg");
        File selectedMap = write(build, "maps/user/world_1/dimension/map.dat");
        File other = write(build, "maps/user/other_world/map.dat");
        String root = FileUtils.getRelativePath(build);

        Set<File> files = absolute(FileUtils.findMatching(root + "/config/*.cfg"));
        files.addAll(absolute(FileUtils.findMatching(root + "/maps/*/world_*/**")));

        assertTrue(files.contains(selected.getAbsoluteFile()));
        assertTrue(files.contains(selectedMap.getAbsoluteFile()));
        assertFalse(files.contains(ignored.getAbsoluteFile()));
        assertFalse(files.contains(other.getAbsoluteFile()));

        // optional includes that do not exist are ignored
        assertTrue(FileUtils.findMatching(root + "/missing-optional/**").isEmpty());
    }

    @Test
    public void findMatching_SkipsUnreadableDirectoriesTheGlobCannotMatch() throws IOException {
        File selected = write(build, "glob/server.cfg");
        File unrelated = FileUtils.ensureExists(new File(build, "glob/private"));
        File matchingName = FileUtils.ensureExists(new File(build, "glob/private.cfg"));
        Assume.assumeNotNull(Files.getFileAttributeView(unrelated.toPath(), PosixFileAttributeView.class));
        String root = FileUtils.getRelativePath(new File(build, "glob"));

        try {
            for (File dir : new File[] { unrelated, matchingName }) {
                Files.setPosixFilePermissions(dir.toPath(), PosixFilePermissions.fromString("---------"));
            }
            // root ignores permissions
            Assume.assumeTrue(unrelated.list() == null);

            assertEquals(
                    Collections.singleton(selected.getAbsoluteFile()),
                    absolute(FileUtils.findMatching(root + "/*.cfg")));
            // A pattern that could match inside an unreadable directory must still fail
            assertThrows(AccessDeniedException.class, () -> FileUtils.findMatching(root + "/**"));
        } finally {
            for (File dir : new File[] { unrelated, matchingName }) {
                Files.setPosixFilePermissions(dir.toPath(), PosixFilePermissions.fromString("rwx------"));
            }
        }
    }

    @Test
    public void scan_WhenWorldDirectoryUnreadable_Throws() throws IOException {
        File unreadable = FileUtils.ensureExists(new File(world, "sub"));
        write(unreadable, "data.dat");
        Assume.assumeNotNull(Files.getFileAttributeView(unreadable.toPath(), PosixFileAttributeView.class));

        try {
            Files.setPosixFilePermissions(unreadable.toPath(), PosixFilePermissions.fromString("---------"));
            // root ignores permissions
            Assume.assumeTrue(unreadable.list() == null);

            // A world backup silently missing files is worse than a failed one
            IOException exception = assertThrows(
                    IOException.class,
                    () -> BackupCandidates.scanWorld(world, backups, false));
            assertEquals("Cannot list backup directory: " + unreadable, exception.getMessage());
        } finally {
            Files.setPosixFilePermissions(unreadable.toPath(), PosixFilePermissions.fromString("rwx------"));
        }
    }

    private static File write(File root, String path) throws IOException {
        File file = new File(root, path);
        FileUtils.ensureExists(file.getParentFile());
        Files.write(file.toPath(), path.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static Set<File> absolute(Collection<File> files) {
        return files.stream().map(f -> f.toPath().toAbsolutePath().normalize().toFile()).collect(Collectors.toSet());
    }

    private static Set<File> files(BackupCandidates candidates) {
        return candidates.getFiles().stream().map(WorldFile::backing).collect(Collectors.toSet());
    }
}
