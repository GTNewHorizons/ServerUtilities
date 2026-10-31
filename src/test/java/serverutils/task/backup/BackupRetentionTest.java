package serverutils.task.backup;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class BackupRetentionTest {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long MONDAY = Instant.parse("2026-10-05T00:00:00Z").toEpochMilli();

    private static ArchiveEntry entry(String name, String world, long created, boolean custom) {
        return new ArchiveEntry(new File(name), world, created, 10, custom, null);
    }

    private static BackupRetention.Plan<ArchiveEntry> select(List<ArchiveEntry> entries, long now, long maxSize,
            boolean deleteCustom, String... policy) {
        return BackupRetention
                .select(entries, BackupRetention.parse(Arrays.asList(policy)), deleteCustom, maxSize, now);
    }

    private static void assertReason(BackupRetention.Plan<ArchiveEntry> plan, ArchiveEntry entry, String reason,
            String... args) {
        String[] expected = new String[args.length + 1];
        expected[0] = "cmd.backup_prune_reason_" + reason;
        System.arraycopy(args, 0, expected, 1, args.length);
        assertArrayEquals(expected, plan.label(entry));
    }

    @Test
    public void parse_RejectsMalformedRules() {
        assertEquals(
                5,
                BackupRetention.parse(Arrays.asList("1h:all", "1d:30m", "7d:1h", "30d:1d", "forever:1w")).size());
        // Malformed durations are covered by BackupDurationTest.
        for (String invalid : new String[] { "", "1h", "1h:all:extra", "1d:forever", "all:1d", null }) {
            assertThrows(
                    String.valueOf(invalid),
                    IllegalArgumentException.class,
                    () -> BackupRetention.parse(Arrays.asList("1h:all", invalid)));
        }
    }

    @Test
    public void select_ProtectedCustom_DoesNotReplaceLatestOrBucketRepresentatives() {
        ArchiveEntry recent = entry("recent", "a", MONDAY + HOUR, false);
        ArchiveEntry named = entry("checkpoint", "a", MONDAY + 2 * HOUR, true);
        ArchiveEntry old = entry("previous-week", "a", MONDAY - DAY, false);
        ArchiveEntry oldNamed = entry("old-checkpoint", "a", MONDAY - HOUR, true);
        List<ArchiveEntry> entries = Arrays.asList(recent, named, old, oldNamed);

        BackupRetention.Plan<ArchiveEntry> plan = select(entries, MONDAY + 3 * HOUR, 1, false, "forever:1w");
        assertTrue(plan.keep.get(recent).contains("Latest backup for world"));
        assertTrue(plan.keep.get(old).contains("forever:1w"));
        assertEquals("Protected custom backup", plan.keep.get(named));
        assertEquals("Protected custom backup", plan.keep.get(oldNamed));
        assertEquals(0, plan.remainingRotationSize);

        plan = select(entries, MONDAY + 3 * HOUR, 0, true, "forever:1w");
        assertTrue(plan.keep.get(named).contains("Latest backup for world"));
        assertTrue(plan.delete.containsKey(recent));
        assertTrue(plan.delete.containsKey(old));
    }

    @Test
    public void select_FutureAndUnreadable_PreservedAndSizeExempt() {
        ArchiveEntry latest = entry("latest", "a", MONDAY, false);
        ArchiveEntry recent = entry("recent", "a", MONDAY - MINUTE, false);
        ArchiveEntry future = entry("future", "a", MONDAY + DAY, false);
        ArchiveEntry unknown = new ArchiveEntry(new File("unknown"), null, 0, 1_000_000, false, "Unrecognized");

        BackupRetention.Plan<ArchiveEntry> plan = select(
                Arrays.asList(latest, recent, future, unknown),
                MONDAY,
                20,
                true,
                "1h:all");

        assertTrue(plan.delete.isEmpty());
        assertEquals(20, plan.remainingRotationSize);
        assertEquals(1_000_030, plan.remainingSize);
        assertTrue(plan.sizeExempt.containsAll(Arrays.asList(future, unknown)));
        assertTrue(plan.isPreserved(future));
        assertTrue(plan.isPreserved(unknown));
    }

    @Test
    public void select_OverlappingRules_CombineIndependentlyOfOrder() {
        long now = MONDAY + 2 * HOUR;
        ArchiveEntry latest = entry("latest", "a", now, false);
        ArchiveEntry recent = entry("recent", "a", now - 5 * MINUTE, false);
        ArchiveEntry exactCutoff = entry("cutoff", "a", now - HOUR, false);
        ArchiveEntry hourly = entry("hourly", "a", now - HOUR - MINUTE, false);
        ArchiveEntry superseded = entry("superseded", "a", now - HOUR - 2 * MINUTE, false);
        ArchiveEntry old = entry("old", "a", now - 2 * DAY, false);
        List<ArchiveEntry> entries = Arrays.asList(latest, recent, exactCutoff, hourly, superseded, old);

        BackupRetention.Plan<ArchiveEntry> plan = select(entries, now, 0, true, "1h:all", "1d:1h");

        assertEquals(4, plan.keep.size());
        assertTrue(plan.keep.containsKey(exactCutoff));
        assertTrue(plan.keep.containsKey(hourly));
        assertTrue(plan.delete.containsKey(superseded));
        assertTrue(plan.delete.containsKey(old));
        assertEquals(plan.keep.keySet(), select(entries, now, 0, true, "1d:1h", "1h:all").keep.keySet());
        assertEquals(40, plan.remainingSize);
    }

    @Test
    public void select_AfterLongOfflineGap_KeepsLatestPerWorld() {
        ArchiveEntry aLatest = entry("a-latest", "a", MONDAY - 40 * DAY, false);
        ArchiveEntry aOld = entry("a-old", "a", MONDAY - 41 * DAY, false);
        ArchiveEntry bLatest = entry("b-latest", "b", MONDAY - 50 * DAY, false);

        BackupRetention.Plan<ArchiveEntry> plan = select(
                Arrays.asList(aLatest, aOld, bLatest),
                MONDAY,
                1,
                true,
                "1h:all");

        assertEquals(2, plan.keep.size());
        assertTrue(plan.delete.containsKey(aOld));
        assertEquals(20, plan.remainingSize);
        assertEquals(20, plan.remainingRotationSize);
    }

    @Test
    public void select_WeeksStartOnMonday_AndCurrentRepresentativeCanBeReplaced() {
        ArchiveEntry sunday = entry("sunday", "a", MONDAY - MINUTE, false);
        ArchiveEntry monday = entry("monday", "a", MONDAY, false);
        ArchiveEntry tuesday = entry("tuesday", "a", MONDAY + DAY, false);

        BackupRetention.Plan<ArchiveEntry> plan = select(
                Arrays.asList(sunday, monday, tuesday),
                MONDAY + DAY,
                1,
                true,
                "forever:1w");

        assertTrue(plan.keep.containsKey(sunday));
        assertTrue(plan.keep.containsKey(tuesday));
        assertTrue(plan.delete.containsKey(monday));
        assertEquals(20, plan.remainingSize);
        assertEquals(0, plan.remainingRotationSize);
    }

    @Test
    public void select_RepeatedPruning_PreservesHistoricalWeeklyRepresentatives() {
        List<ArchiveEntry> surviving = new ArrayList<>();
        for (int hour = 0; hour <= 24 * 35; hour++) {
            long now = MONDAY + hour * HOUR;
            surviving.add(entry("hour-" + hour, "a", now, false));
            BackupRetention.Plan<ArchiveEntry> plan = select(surviving, now, 0, true, "1d:all", "forever:1w");
            surviving.removeIf(plan.delete::containsKey);
            assertEquals(plan.keep.keySet(), select(surviving, now, 0, true, "1d:all", "forever:1w").keep.keySet());
        }
        List<String> ids = surviving.stream().map(ArchiveEntry::id).collect(Collectors.toList());
        for (int week = 0; week < 5; week++) {
            assertTrue(ids.contains("hour-" + ((week + 1) * 168 - 1)));
        }
    }

    @Test
    public void select_SizeLimit_RemovesOnlyFiniteRetainedOldestFirst() {
        ArchiveEntry weekly = entry("weekly", "a", MONDAY - DAY, false);
        ArchiveEntry finite = entry("finite", "a", MONDAY + MINUTE, false);
        ArchiveEntry recent = entry("recent", "a", MONDAY + 2 * MINUTE, false);
        ArchiveEntry named = entry("named", "a", MONDAY + 3 * MINUTE, true);
        ArchiveEntry latest = entry("latest", "a", MONDAY + 4 * MINUTE, false);
        List<ArchiveEntry> entries = Arrays.asList(weekly, finite, recent, named, latest);
        long now = latest.createdAt();

        BackupRetention.Plan<ArchiveEntry> plan = select(entries, now, 20, false, "1d:all", "forever:1w");
        assertTrue(plan.delete.isEmpty());
        assertEquals(50, plan.remainingSize);
        assertEquals(20, plan.remainingRotationSize);

        plan = select(entries, now, 10, false, "1d:all", "forever:1w");
        assertEquals(Collections.singleton(finite), plan.delete.keySet());
        assertEquals("Folder size limit", plan.delete.get(finite));
        assertEquals(40, plan.remainingSize);
        assertEquals(10, plan.remainingRotationSize);

        plan = select(entries, now, 1, false, "1d:all", "forever:1w");
        assertTrue(plan.keep.containsKey(weekly));
        assertTrue(plan.keep.containsKey(named));
        assertTrue(plan.keep.containsKey(latest));
        assertEquals(30, plan.remainingSize);
        assertEquals(0, plan.remainingRotationSize);
    }

    @Test
    public void select_CustomProtection_ExemptFromSizeOnlyWhenEnabled() {
        ArchiveEntry named = entry("named", "a", MONDAY, true);
        ArchiveEntry recent = entry("recent", "a", MONDAY + MINUTE, false);
        ArchiveEntry latest = entry("latest", "a", MONDAY + 2 * MINUTE, false);
        List<ArchiveEntry> entries = Arrays.asList(named, recent, latest);
        long now = latest.createdAt();

        BackupRetention.Plan<ArchiveEntry> plan = select(entries, now, 20, false, "1d:all");
        assertTrue(plan.delete.isEmpty());
        assertEquals(30, plan.remainingSize);
        assertEquals(20, plan.remainingRotationSize);

        plan = select(entries, now, 20, true, "1d:all");
        assertEquals(Collections.singleton(named), plan.delete.keySet());
        assertEquals(20, plan.remainingRotationSize);

        plan = select(entries, now, 1, false, "1d:all");
        assertEquals(Collections.singleton(recent), plan.delete.keySet());
        assertEquals(20, plan.remainingSize);
        assertEquals(10, plan.remainingRotationSize);

        // An entry both custom-protected and kept forever is excluded from the size count once.
        List<ArchiveEntry> forever = Arrays.asList(named, entry("latest", "a", MONDAY + MINUTE, false));
        for (boolean deleteCustom : new boolean[] { false, true }) {
            plan = select(forever, MONDAY + MINUTE, 1, deleteCustom, "1d:all", "forever:all");
            assertTrue(plan.delete.isEmpty());
            assertEquals(20, plan.remainingSize);
            assertEquals(0, plan.remainingRotationSize);
        }
    }

    @Test
    public void select_Labels_ExplainEachDecisionOldestFirst() {
        long now = MONDAY + 2 * DAY;
        ArchiveEntry ancientReplaced = entry("ancient-replaced", "a", MONDAY - 8 * DAY - HOUR, false);
        ArchiveEntry ancient = entry("ancient", "a", MONDAY - 8 * DAY, false);
        ArchiveEntry replaced = entry("replaced", "a", now - 3 * HOUR, false);
        ArchiveEntry sampled = entry("sampled", "a", now - 3 * HOUR + 5 * MINUTE, false);
        ArchiveEntry named = entry("named", "a", now - 2 * HOUR, true);
        ArchiveEntry recent = entry("recent", "a", now - 2 * MINUTE, false);
        ArchiveEntry latest = entry("latest", "a", now - MINUTE, false);
        List<ArchiveEntry> entries = Arrays.asList(latest, ancient, named, recent, ancientReplaced, sampled, replaced);

        BackupRetention.Plan<ArchiveEntry> plan = select(entries, now, 0, false, "1h:all", "1d:30m", "forever:1w");

        assertEquals(
                Arrays.asList("ancient-replaced", "ancient", "replaced", "sampled", "named", "recent", "latest"),
                plan.entries().stream().map(ArchiveEntry::id).collect(Collectors.toList()));
        assertReason(plan, latest, "latest_rule", "forever:1w");
        assertEquals(-1L, plan.keptUntil(latest));
        assertReason(plan, recent, "rule", "1h:all");
        assertEquals(recent.createdAt() + HOUR, plan.keptUntil(recent));
        assertReason(plan, sampled, "rule", "1d:30m");
        assertEquals(sampled.createdAt() + DAY, plan.keptUntil(sampled));
        assertReason(plan, replaced, "replaced", "1d:30m");
        assertReason(plan, ancient, "rule", "forever:1w");
        assertEquals(-1L, plan.keptUntil(ancient));
        assertReason(plan, ancientReplaced, "replaced", "forever:1w");
        assertReason(plan, named, "custom");
        assertTrue(plan.isPreserved(named));
        assertFalse(plan.isPreserved(latest));

        plan = select(Arrays.asList(latest, sampled), now, 0, false, "1h:all");
        assertReason(plan, sampled, "older");
        plan = select(Arrays.asList(latest, recent), now, 1, false, "1d:all");
        assertReason(plan, recent, "size");
    }
}
