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

public class RetentionRoutineTest extends BackupFixtures.Tests {

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
        // Everything is inside the policy, so only the size allowance can delete
        FakeEntry latest = new FakeEntry("latest", "a", now, false, null);
        FakeEntry fallback = new FakeEntry("fallback", "a", now - 1000, false, null);
        FakeEntry blocked = new FakeEntry("blocked", "a", now - 2000, false, null);
        FakeEntry otherWorld = new FakeEntry("other-world", "b", now - 500, false, null);
        FakeEntry custom = new FakeEntry("custom", "a", now - 3000, true, null);
        FakeEntry future = new FakeEntry("future", "a", now + TimeUnit.DAYS.toMillis(1), false, null);
        FakeEntry unreadable = new FakeEntry("unreadable", null, 0, false, "Unrecognized/unreadable archive");
        FakeStore store = new FakeStore(latest, fallback, blocked, otherWorld, custom, future, unreadable);
        store.failing.add(blocked);

        RetentionRoutine.Options options = new RetentionRoutine.Options(Collections.singletonList("1d:all"), false, 0, 1);
        options.maxSize = 30;

        BackupResult.Retention result = new RetentionRoutine(store, options).prune();

        assertNull(result.error);
        assertEquals(Collections.singletonList(fallback), store.deleted);
    }

    @Test
    public void prune_WhenCancelled_DeletesNothing() {
        FakeStore store = new FakeStore(
                new FakeEntry("latest", "a", now, false, null),
                new FakeEntry("old", "a", now - TimeUnit.DAYS.toMillis(2), false, null));
        RetentionRoutine routine = new RetentionRoutine(
                store,
                new RetentionRoutine.Options(Collections.singletonList("1h:all"), false, 0, 1));

        // Cancelling before the run starts interrupts the run as soon as it begins.
        routine.cancel();
        BackupResult.Retention result;
        try {
            result = routine.prune();
        } finally {
            Thread.interrupted();
        }

        assertTrue(result.error instanceof InterruptedIOException);
        assertTrue(store.deleted.isEmpty());
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
        private final boolean custom;
        private final String problem;

        FakeEntry(String id, @Nullable String world, long createdAt, boolean custom, @Nullable String problem) {
            this.id = id;
            this.world = world;
            this.createdAt = createdAt;
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
            return 10;
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