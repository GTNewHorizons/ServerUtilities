package serverutils.task.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import serverutils.lib.util.FileUtils;

public class RetentionRoutineTest extends BackupFixtures.Tests {

    private static final List<List<String>> POLICY_AND_LEGACY = Arrays
            .asList(Collections.singletonList("1d:all"), Collections.<String>emptyList());
    /** Legacy rotation modes: 0 counts backups, a positive limit counts bytes. */
    private static final long[] COUNT_AND_SIZE = { 0, 1 };

    private final String worldId = UUID.randomUUID().toString();
    private final long now = System.currentTimeMillis();

    @Before
    public void beforeEach() {
        testSetup(SetupFlags.FOLDERS);
    }

    @After
    public void afterEach() {
        testReset();
    }

    @Test
    public void deleteAbandonedArchives_DeletesOnlyStagingFiles() throws IOException {
        File folder = FileUtils.ensureExists(new File(build, "staging"));
        File orphan = write(new File(folder, ".su-save-123.tmp"));
        File archive = write(new File(folder, "completed.zip"));
        File unrelated = write(new File(folder, "other.tmp"));
        File contents = write(new File(folder, ".su-save-directory.tmp/keep"));

        RetentionRoutine.deleteAbandonedArchives(folder);

        assertFalse(orphan.exists());
        assertTrue(archive.exists());
        assertTrue(unrelated.exists());
        assertTrue(contents.exists());

        // nothing left to reclaim, so a second run changes nothing
        RetentionRoutine.deleteAbandonedArchives(folder);
        assertTrue(archive.exists());
    }

    private static File write(File file) throws IOException {
        FileUtils.ensureExists(file.getParentFile());
        Files.write(file.toPath(), new byte[] { 1 });
        return file;
    }

    private RetentionRoutine routine(String... policy) {
        return new RetentionRoutine(
                new ArchiveStore(backups),
                new RetentionRoutine.Options(Arrays.asList(policy), true, 0, 1));
    }

    @Test
    public void prune_WhenPolicyInvalid_DeletesNothing() throws IOException {
        File old = new File(backups, "old.zip");
        File latest = new File(backups, "latest.zip");
        BackupFixtures.writeArchive(old, worldId, now - TimeUnit.DAYS.toMillis(2));
        BackupFixtures.writeArchive(latest, worldId, now);

        BackupResult.Retention result = routine("1h:all", "invalid").prune();

        assertTrue(result.error instanceof IllegalArgumentException);
        assertTrue(old.exists());
        assertTrue(latest.exists());
    }

    @Test
    public void preview_MatchesWhatPruneDeletes() throws IOException {
        File old = new File(backups, "old.zip");
        File latest = new File(backups, "latest.zip");
        BackupFixtures.writeArchive(old, worldId, now - TimeUnit.DAYS.toMillis(2));
        BackupFixtures.writeArchive(latest, worldId, now);
        RetentionRoutine routine = routine("1h:all");

        BackupRetention.Plan<? extends BackupEntry> plan = routine.preview();
        assertEquals(Collections.singleton(old), files(plan.delete.keySet()));
        assertEquals(Collections.singleton(latest), files(plan.keep.keySet()));
        assertTrue("Preview must not delete anything", old.exists());

        assertNull(routine.prune().error);
        assertFalse(old.exists());
        assertTrue(latest.exists());
    }

    @Test
    public void prune_WhenSizeDeletionFails_TriesNextCandidateAndSparesProtected() {
        // In policy mode everything is inside the policy, so only the size allowance can delete
        for (List<String> policy : POLICY_AND_LEGACY) {
            FakeEntry latest = new FakeEntry("latest", "a", now, false, null);
            FakeEntry fallback = new FakeEntry("fallback", "a", now - 1000, false, null);
            FakeEntry blocked = new FakeEntry("blocked", "a", now - 2000, false, null);
            FakeEntry otherWorld = new FakeEntry("other-world", "b", now - 500, false, null);
            FakeEntry custom = new FakeEntry("custom", "a", now - 3000, true, null);
            FakeEntry future = new FakeEntry("future", "a", now + TimeUnit.DAYS.toMillis(1), false, null);
            FakeEntry unreadable = new FakeEntry("unreadable", null, 0, false, "Unrecognized/unreadable archive");
            FakeStore store = new FakeStore(latest, fallback, blocked, otherWorld, custom, future, unreadable);
            store.failing.add(blocked);

            RetentionRoutine.Options options = new RetentionRoutine.Options(policy, false, 0, 1);
            options.maxSize = 30;

            BackupResult.Retention result = new RetentionRoutine(store, options).prune();

            assertNull(result.error);
            assertEquals(policy.toString(), Collections.singletonList(fallback), store.deleted);
        }
    }

    @Test
    public void prune_WhenCancelled_DeletesNothing() {
        for (List<String> policy : POLICY_AND_LEGACY) {
            FakeStore store = new FakeStore(
                    new FakeEntry("latest", "a", now, false, null),
                    new FakeEntry("old", "a", now - TimeUnit.DAYS.toMillis(2), false, null));
            RetentionRoutine routine = new RetentionRoutine(store, new RetentionRoutine.Options(policy, false, 0, 1));

            // Cancelling before the run starts interrupts the run as soon as it begins.
            routine.cancel();
            BackupResult.Retention result;
            try {
                result = routine.prune();
            } finally {
                Thread.interrupted();
            }

            assertTrue(policy.toString(), result.error instanceof InterruptedIOException);
            assertTrue(store.deleted.isEmpty());
        }
    }

    @Test
    public void pruneBySize_KeepsNewestPerWorld() {
        for (long maxSize : COUNT_AND_SIZE) {
            FakeEntry aOld = new FakeEntry("a-old", "a", now - 4000, false, null);
            FakeEntry bOld = new FakeEntry("b-old", "b", now - 3000, false, null);
            FakeStore store = new FakeStore(
                    aOld,
                    new FakeEntry("a-latest", "a", now - 2000, false, null),
                    bOld,
                    new FakeEntry("b-latest", "b", now - 1000, false, null));

            assertNull(legacy(store, 1, maxSize, true).error);

            assertEquals(Arrays.asList(aOld, bOld), store.deleted);
        }
    }

    @Test
    public void pruneBySize_PreservedEntriesNeverCountOrDisplaceLatest() {
        for (long maxSize : COUNT_AND_SIZE) {
            for (boolean deleteCustom : new boolean[] { false, true }) {
                FakeEntry custom = new FakeEntry("custom", "a", now - 3000, true, null);
                FakeEntry old = new FakeEntry("old", "a", now - 2000, false, null);
                FakeStore store = new FakeStore(
                        custom,
                        old,
                        new FakeEntry("latest", "a", now - 1000, false, null),
                        new FakeEntry("future", "a", now + TimeUnit.DAYS.toMillis(1), false, null),
                        new FakeEntry("unreadable", null, now, false, "Unsupported or invalid backup metadata"));

                assertNull(legacy(store, 1, maxSize, deleteCustom).error);

                assertEquals(deleteCustom ? Arrays.asList(custom, old) : Collections.singletonList(old), store.deleted);
            }
        }
    }

    @Test
    public void pruneBySize_EqualTimestamps_OrderedByLabelNotListOrder() {
        for (long maxSize : COUNT_AND_SIZE) {
            FakeEntry a = new FakeEntry("a", "w", now, 8);
            FakeEntry b = new FakeEntry("b", "w", now, 8);
            FakeStore store = new FakeStore(new FakeEntry("c", "w", now, 1), a, b);

            assertNull(legacy(store, 1, maxSize, true).error);

            assertEquals(Arrays.asList(a, b), store.deleted);
        }
    }

    @Test
    public void pruneBySize_NewestOverLimitAndInvalidCount_NeverDeleted() {
        for (int keepCount : new int[] { 1, 0, -1 }) {
            FakeEntry old = new FakeEntry("old", "a", now - 1000, 10);
            FakeStore store = new FakeStore(old, new FakeEntry("newest", "a", now, 10));

            // A valid count runs size mode with an allowance smaller than the newest alone; invalid counts clamp to 1
            assertNull(legacy(store, keepCount, keepCount == 1 ? 1 : 0, true).error);

            assertEquals(Collections.singletonList(old), store.deleted);
        }
    }

    @Test
    public void pruneBySize_NegativeSizeLimit_Skips() {
        FakeStore store = new FakeStore(
                new FakeEntry("old", "a", now - 1000, false, null),
                new FakeEntry("latest", "a", now, false, null));

        assertNull(legacy(store, 1, -1, true).error);

        assertTrue(store.deleted.isEmpty());
    }

    @Test
    public void pruneBySize_SizeLimitTakesPrecedenceOverCount() {
        FakeEntry old = new FakeEntry("old", "a", now - 1000, 10);
        FakeStore store = new FakeStore(old, new FakeEntry("latest", "a", now, 1));

        assertNull(legacy(store, 1, 11, true).error);
        assertTrue("Both fit the size allowance despite exceeding the count", store.deleted.isEmpty());

        assertNull(legacy(store, 1, 0, true).error);
        assertEquals(Collections.singletonList(old), store.deleted);
    }

    /** Runs legacy count/size rotation; a maxSize of 0 means count mode. */
    private static BackupResult.Retention legacy(FakeStore store, int keepCount, long maxSize, boolean deleteCustom) {
        RetentionRoutine.Options options = new RetentionRoutine.Options(
                Collections.emptyList(),
                deleteCustom,
                0,
                keepCount);
        options.maxSize = maxSize;
        return new RetentionRoutine(store, options).prune();
    }

    @Test
    public void prune_PolicyMode_IdentifiesLegacyArchiveWorldFromNestedUniverse() throws IOException {
        File[] archives = writeNestedWorldArchives();

        assertNull(routine("1h:all").prune().error);

        assertFalse("Legacy archive must be recognized as the same world and pruned", archives[0].exists());
        assertTrue(archives[1].exists());
    }

    @Test
    public void prune_LegacyMode_IdentifiesLegacyArchiveWorldFromNestedUniverse() throws IOException {
        File[] archives = writeNestedWorldArchives();

        assertNull(routine().prune().error);

        assertFalse("Legacy archive must be recognized as the same world and pruned", archives[0].exists());
        assertTrue(archives[1].exists());
    }

    private File[] writeNestedWorldArchives() throws IOException {
        File old = new File(backups, "nested-legacy.zip");
        File latest = new File(backups, "nested-modern.zip");
        BackupFixtures.writeLegacyArchive(old, "saves/worlds/world", UUID.fromString(worldId));
        Files.setLastModifiedTime(old.toPath(), FileTime.fromMillis(1));
        BackupFixtures.writeArchive(latest, worldId, now);
        return new File[] { old, latest };
    }

    private static Set<File> files(Collection<? extends BackupEntry> entries) {
        return entries.stream().map(entry -> ((ArchiveEntry) entry).file).collect(Collectors.toSet());
    }

    private static final class FakeEntry implements BackupEntry {

        private final String id;
        private final String world;
        private final long createdAt;
        private final long size;
        private final boolean custom;
        private final String problem;

        FakeEntry(String id, @Nullable String world, long createdAt, boolean custom, @Nullable String problem) {
            this(id, world, createdAt, 10, custom, problem);
        }

        FakeEntry(String id, String world, long createdAt, long size) {
            this(id, world, createdAt, size, false, null);
        }

        private FakeEntry(String id, @Nullable String world, long createdAt, long size, boolean custom,
                @Nullable String problem) {
            this.id = id;
            this.world = world;
            this.createdAt = createdAt;
            this.size = size;
            this.custom = custom;
            this.problem = problem;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String world() {
            return world;
        }

        @Override
        public long createdAt() {
            return createdAt;
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public boolean isCustom() {
            return custom;
        }

        @Override
        public String problem() {
            return problem;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private static final class FakeStore implements BackupStore<FakeEntry> {

        final List<FakeEntry> entries;
        final Set<FakeEntry> failing = new HashSet<>();
        final List<FakeEntry> deleted = new ArrayList<>();

        FakeStore(FakeEntry... entries) {
            this.entries = Arrays.asList(entries);
        }

        @Override
        public List<FakeEntry> list() {
            return entries;
        }

        @Override
        public void delete(Collection<FakeEntry> entries, BackupResult.Retention result) {
            for (FakeEntry entry : entries) {
                if (failing.contains(entry)) continue;
                deleted.add(entry);
                result.filesRemoved++;
                result.bytesRemoved += entry.size();
            }
        }
    }
}
