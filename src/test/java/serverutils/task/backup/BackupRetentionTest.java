package serverutils.task.backup;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import serverutils.lib.util.compression.CommonsCompressor;
import serverutils.lib.util.compression.ICompress;
import serverutils.lib.util.compression.LegacyCompressor;

public class BackupRetentionTest {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long MONDAY = Instant.parse("2026-10-05T00:00:00Z").toEpochMilli();
    private static final String WORLD = "12345678-1234-1234-1234-123456789abc";

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private static BackupRetention.Archive archive(String name, String world, long time, boolean custom) {
        return new BackupRetention.Archive(new File(name), world, time, 10, custom, null);
    }

    private static BackupRetention.Plan select(List<BackupRetention.Archive> archives, long now, long limit,
            boolean deleteCustom, String... policy) {
        return BackupRetention.select(archives, BackupRetention.parse(policy), now, deleteCustom, limit);
    }

    @Test
    public void protectedCustomBackupsDoNotReplaceLatestOrBucketRepresentatives() {
        BackupRetention.Archive recent = archive("recent", "a", MONDAY + HOUR, false);
        BackupRetention.Archive named = archive("checkpoint", "a", MONDAY + 2 * HOUR, true);
        BackupRetention.Archive old = archive("previous-week", "a", MONDAY - DAY, false);
        BackupRetention.Archive oldNamed = archive("old-checkpoint", "a", MONDAY - HOUR, true);
        List<BackupRetention.Archive> archives = Arrays.asList(recent, named, old, oldNamed);
        BackupRetention.Plan plan = select(archives, MONDAY + 3 * HOUR, 1, false, "forever:1w");
        assertTrue(plan.keep.get(recent.file).contains("Latest backup for world"));
        assertTrue(plan.keep.get(old.file).contains("forever:1w"));
        assertEquals("Protected custom backup", plan.keep.get(named.file));
        assertEquals("Protected custom backup", plan.keep.get(oldNamed.file));
        assertEquals(0, plan.remainingRotationSize);
        plan = select(archives, MONDAY + 3 * HOUR, 0, true, "forever:1w");
        assertTrue(plan.keep.get(named.file).contains("Latest backup for world"));
        assertTrue(plan.delete.containsKey(recent.file));
        assertTrue(plan.delete.containsKey(old.file));
    }

    @Test
    public void unrecognizedAndFutureArchivesDoNotEvictFiniteHistory() {
        BackupRetention.Archive latest = archive("latest", "a", MONDAY, false);
        BackupRetention.Archive recent = archive("recent", "a", MONDAY - MINUTE, false);
        BackupRetention.Archive future = archive("future", "a", MONDAY + DAY, false);
        BackupRetention.Archive unknown = new BackupRetention.Archive(
                new File("unknown"),
                null,
                0,
                1_000_000,
                false,
                "Unrecognized/unreadable");
        BackupRetention.Plan plan = select(Arrays.asList(latest, recent, future, unknown), MONDAY, 20, true, "1h:all");
        assertTrue(plan.delete.isEmpty());
        assertEquals(20, plan.remainingRotationSize);
        assertEquals(1_000_030, plan.remainingSize);
        assertTrue(plan.sizeExempt.containsAll(Arrays.asList(future.file, unknown.file)));
    }

    @Test
    public void recognizesUncommentedWorldFoldersWithoutGuessingAmbiguousOrUnsafeLayouts() throws Exception {
        Path old = zip("old.zip", null, "worlds/world/level.dat", new byte[] { 1 }, false);
        Path latest = zip("latest.zip", null, "worlds/world/level.dat_old", new byte[] { 1 }, false);
        Path other = zip("other.zip", null, "worlds/other/level.dat", new byte[] { 1 }, false);
        Files.setLastModifiedTime(old, FileTime.fromMillis(1));
        Files.setLastModifiedTime(latest, FileTime.fromMillis(2));
        Files.setLastModifiedTime(other, FileTime.fromMillis(3));
        Path unsafe = zip("unsafe.zip", null, "../world/level.dat", new byte[] { 1 }, false);
        Path ambiguous = temporary.getRoot().toPath().resolve("ambiguous.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(ambiguous))) {
            for (String prefix : new String[] { "world/", "other/" }) {
                zip.putNextEntry(new ZipEntry(prefix + "level.dat"));
                zip.write(1);
            }
        }
        BackupRetention.Plan plan = BackupRetention
                .plan(temporary.getRoot(), new String[] { "1h:all" }, MONDAY, true, 1);
        assertTrue(plan.delete.containsKey(old.toFile()));
        assertTrue(plan.keep.get(latest.toFile()).contains("Latest backup for world"));
        assertTrue(plan.keep.get(other.toFile()).contains("Latest backup for world"));
        for (Path rejected : Arrays.asList(unsafe, ambiguous)) {
            assertTrue(plan.keep.get(rejected.toFile()).contains("Unrecognized/unreadable"));
        }
        assertEquals("name:world", BackupRetention.read(latest.toFile(), Files.size(latest)).world);
    }

    @Test
    public void validatesEveryRuleBeforeSelectingAnything() {
        assertEquals(
                5,
                BackupRetention.parse(new String[] { "1h:all", "1d:30m", "7d:1h", "30d:1d", "forever:1w" }).size());
        for (String invalid : new String[] { "", "1h", "1h:all:extra", "0h:all", "-1d:1h", "1d:0m", "1month:1d",
                "1d:forever", "all:1d", "1d:1.5h", "99999999999999999999w:all", null }) {
            assertThrows(
                    invalid,
                    IllegalArgumentException.class,
                    () -> BackupRetention.parse(new String[] { "1h:all", invalid }));
        }
    }

    @Test
    public void combinesOverlappingRulesIndependentlyOfTheirOrder() {
        long now = MONDAY + 2 * HOUR;
        BackupRetention.Archive latest = archive("latest", "a", now, false);
        BackupRetention.Archive recent = archive("recent", "a", now - 5 * MINUTE, false);
        BackupRetention.Archive exactCutoff = archive("cutoff", "a", now - HOUR, false);
        BackupRetention.Archive hourly = archive("hourly", "a", now - HOUR - MINUTE, false);
        BackupRetention.Archive superseded = archive("superseded", "a", now - HOUR - 2 * MINUTE, false);
        BackupRetention.Archive old = archive("old", "a", now - 2 * DAY, false);
        List<BackupRetention.Archive> input = Arrays.asList(latest, recent, exactCutoff, hourly, superseded, old);
        BackupRetention.Plan plan = select(input, now, 0, true, "1h:all", "1d:1h");
        assertEquals(4, plan.keep.size());
        assertTrue(plan.keep.containsKey(exactCutoff.file));
        assertTrue(plan.keep.containsKey(hourly.file));
        assertTrue(plan.delete.containsKey(superseded.file));
        assertTrue(plan.delete.containsKey(old.file));
        assertEquals(plan.keep.keySet(), select(input, now, 0, true, "1d:1h", "1h:all").keep.keySet());
        assertEquals(40, plan.remainingSize);
    }

    @Test
    public void protectsLatestPerWorldAfterLongOfflineGaps() {
        List<BackupRetention.Archive> archives = Arrays.asList(
                archive("a-latest", "a", MONDAY - 40 * DAY, false),
                archive("a-old", "a", MONDAY - 41 * DAY, false),
                archive("b-latest", "b", MONDAY - 50 * DAY, false));
        BackupRetention.Plan plan = select(archives, MONDAY, 1, true, "1h:all");
        assertEquals(2, plan.keep.size());
        assertTrue(plan.delete.containsKey(new File("a-old")));
        assertEquals(20, plan.remainingSize);
        assertEquals(20, plan.remainingRotationSize);
    }

    @Test
    public void weeksStartOnMondayAndCurrentRepresentativeCanBeReplaced() {
        BackupRetention.Archive sunday = archive("sunday", "a", MONDAY - MINUTE, false);
        BackupRetention.Archive monday = archive("monday", "a", MONDAY, false);
        BackupRetention.Archive tuesday = archive("tuesday", "a", MONDAY + DAY, false);
        BackupRetention.Plan plan = select(Arrays.asList(sunday, monday, tuesday), MONDAY + DAY, 1, true, "forever:1w");
        assertTrue(plan.keep.containsKey(sunday.file));
        assertTrue(plan.keep.containsKey(tuesday.file));
        assertTrue(plan.delete.containsKey(monday.file));
        assertEquals(20, plan.remainingSize);
        assertEquals(0, plan.remainingRotationSize);
    }

    @Test
    public void repeatedPruningPreservesHistoricalWeeklyRepresentatives() {
        List<BackupRetention.Archive> surviving = new ArrayList<>();
        for (int hour = 0; hour <= 24 * 35; hour++) {
            long now = MONDAY + hour * HOUR;
            surviving.add(archive("hour-" + hour, "a", now, false));
            BackupRetention.Plan plan = select(surviving, now, 0, true, "1d:all", "forever:1w");
            surviving.removeIf(archive -> plan.delete.containsKey(archive.file));
            assertEquals(plan.keep.keySet(), select(surviving, now, 0, true, "1d:all", "forever:1w").keep.keySet());
        }
        for (int week = 0; week < 5; week++) {
            File expected = new File("hour-" + ((week + 1) * 168 - 1));
            assertTrue(surviving.stream().anyMatch(archive -> archive.file.equals(expected)));
        }
    }

    @Test
    public void sizeLimitRemovesOnlyFiniteRetainedBackupsOldestFirst() {
        BackupRetention.Archive weekly = archive("weekly", "a", MONDAY - DAY, false);
        BackupRetention.Archive finite = archive("finite", "a", MONDAY + MINUTE, false);
        BackupRetention.Archive recent = archive("recent", "a", MONDAY + 2 * MINUTE, false);
        BackupRetention.Archive named = archive("named", "a", MONDAY + 3 * MINUTE, true);
        BackupRetention.Archive latest = archive("latest", "a", MONDAY + 4 * MINUTE, false);
        List<BackupRetention.Archive> input = Arrays.asList(weekly, finite, recent, named, latest);
        BackupRetention.Plan plan = select(input, latest.created, 20, false, "1d:all", "forever:1w");
        assertTrue(plan.delete.isEmpty());
        assertEquals(50, plan.remainingSize);
        assertEquals(20, plan.remainingRotationSize);
        plan = select(input, latest.created, 10, false, "1d:all", "forever:1w");
        assertEquals(Collections.singleton(finite.file), plan.delete.keySet());
        assertEquals("Folder size limit", plan.delete.get(finite.file));
        assertEquals(40, plan.remainingSize);
        assertEquals(10, plan.remainingRotationSize);
        plan = select(input, latest.created, 1, false, "1d:all", "forever:1w");
        assertTrue(plan.keep.containsKey(weekly.file));
        assertTrue(plan.keep.containsKey(named.file));
        assertTrue(plan.keep.containsKey(latest.file));
        assertEquals(30, plan.remainingSize);
        assertEquals(0, plan.remainingRotationSize);
    }

    @Test
    public void customProtectionExcludesSizeOnlyWhenEnabledAndLatestStillCounts() {
        BackupRetention.Archive named = archive("named", "a", MONDAY, true);
        BackupRetention.Archive recent = archive("recent", "a", MONDAY + MINUTE, false);
        BackupRetention.Archive latest = archive("latest", "a", MONDAY + 2 * MINUTE, false);
        List<BackupRetention.Archive> input = Arrays.asList(named, recent, latest);
        BackupRetention.Plan plan = select(input, latest.created, 20, false, "1d:all");
        assertTrue(plan.delete.isEmpty());
        assertEquals(30, plan.remainingSize);
        assertEquals(20, plan.remainingRotationSize);
        plan = select(input, latest.created, 20, true, "1d:all");
        assertEquals(Collections.singleton(named.file), plan.delete.keySet());
        assertEquals(20, plan.remainingRotationSize);
        plan = select(input, latest.created, 1, false, "1d:all");
        assertEquals(Collections.singleton(recent.file), plan.delete.keySet());
        assertEquals(20, plan.remainingSize);
        assertEquals(10, plan.remainingRotationSize);
    }

    private static void assertReason(BackupRetention.Plan plan, File file, String reason, String... args) {
        String[] expected = new String[args.length + 1];
        expected[0] = "cmd.backup_prune_reason_" + reason;
        System.arraycopy(args, 0, expected, 1, args.length);
        assertArrayEquals(expected, plan.label(file));
    }

    @Test
    public void previewLabelsExplainEachDecisionOldestFirst() {
        long now = MONDAY + 2 * DAY;
        BackupRetention.Archive ancientReplaced = archive("ancient-replaced", "a", MONDAY - 8 * DAY - HOUR, false);
        BackupRetention.Archive ancient = archive("ancient", "a", MONDAY - 8 * DAY, false);
        BackupRetention.Archive replaced = archive("replaced", "a", now - 3 * HOUR, false);
        BackupRetention.Archive sampled = archive("sampled", "a", now - 3 * HOUR + 5 * MINUTE, false);
        BackupRetention.Archive named = archive("named", "a", now - 2 * HOUR, true);
        BackupRetention.Archive recent = archive("recent", "a", now - 2 * MINUTE, false);
        BackupRetention.Archive latest = archive("latest", "a", now - MINUTE, false);
        List<BackupRetention.Archive> input = Arrays
                .asList(latest, ancient, named, recent, ancientReplaced, sampled, replaced);
        BackupRetention.Plan plan = select(input, now, 0, false, "1h:all", "1d:30m", "forever:1w");

        List<String> order = new java.util.ArrayList<>();
        for (BackupRetention.Archive archive : plan.archives()) order.add(archive.file.getName());
        assertEquals(
                Arrays.asList("ancient-replaced", "ancient", "replaced", "sampled", "named", "recent", "latest"),
                order);
        assertReason(plan, latest.file, "latest_rule", "forever:1w");
        assertEquals(-1L, plan.keptUntil(latest.file));
        assertReason(plan, recent.file, "rule", "1h:all");
        assertEquals(recent.created + HOUR, plan.keptUntil(recent.file));
        assertReason(plan, sampled.file, "rule", "1d:30m");
        assertEquals(sampled.created + DAY, plan.keptUntil(sampled.file));
        assertReason(plan, replaced.file, "replaced", "1d:30m");
        assertReason(plan, ancient.file, "rule", "forever:1w");
        assertEquals(-1L, plan.keptUntil(ancient.file));
        assertReason(plan, ancientReplaced.file, "replaced", "forever:1w");
        assertReason(plan, named.file, "custom");
        assertTrue(plan.isPreserved(named.file));
        assertFalse(plan.isPreserved(latest.file));

        plan = select(Arrays.asList(latest, sampled), now, 0, false, "1h:all");
        assertReason(plan, sampled.file, "older");
        plan = select(Arrays.asList(latest, recent), now, 1, false, "1d:all");
        assertReason(plan, recent.file, "size");
    }

    @Test
    public void overlappingForeverAndCustomProtectionExcludesEachArchiveOnce() {
        List<BackupRetention.Archive> input = Arrays
                .asList(archive("named", "a", MONDAY, true), archive("latest", "a", MONDAY + MINUTE, false));
        for (boolean deleteCustom : new boolean[] { false, true }) {
            BackupRetention.Plan plan = select(input, MONDAY + MINUTE, 1, deleteCustom, "1d:all", "forever:all");
            assertTrue(plan.delete.isEmpty());
            assertEquals(20, plan.remainingSize);
            assertEquals(0, plan.remainingRotationSize);
        }
    }

    @Test
    public void previewReportsBothSizesAndWarnsOnlyWhenCountedSizeExceedsAllowance() throws Exception {
        long gb = serverutils.lib.util.FileUtils.SizeUnit.GB.getSize();
        List<BackupRetention.Archive> input = Arrays.asList(
                new BackupRetention.Archive(new File("named"), "a", MONDAY, 3 * gb, true, null),
                new BackupRetention.Archive(new File("recent"), "a", MONDAY + MINUTE, gb, false, null),
                new BackupRetention.Archive(new File("latest"), "a", MONDAY + 2 * MINUTE, 2 * gb, false, null));
        java.lang.reflect.Method sendPreview = serverutils.command.CmdBackup.CmdBackupPrune.class.getDeclaredMethod(
                "sendPreview",
                net.minecraft.command.ICommandSender.class,
                BackupRetention.Plan.class,
                Throwable.class);
        sendPreview.setAccessible(true);
        int previousLimit = serverutils.ServerUtilitiesConfig.backups.max_folder_size;
        try {
            serverutils.ServerUtilitiesConfig.backups.max_folder_size = 1;
            for (String policy : new String[] { "forever:all", "1d:all" }) {
                BackupRetention.Plan plan = select(input, MONDAY + 2 * MINUTE, gb, false, policy);
                net.minecraft.command.ICommandSender sender = org.mockito.Mockito
                        .mock(net.minecraft.command.ICommandSender.class);
                sendPreview.invoke(null, sender, plan, null);
                org.mockito.ArgumentCaptor<net.minecraft.util.IChatComponent> replies = org.mockito.ArgumentCaptor
                        .forClass(net.minecraft.util.IChatComponent.class);
                org.mockito.Mockito.verify(sender, org.mockito.Mockito.atLeastOnce()).addChatMessage(replies.capture());
                boolean warned = false;
                boolean summarized = false;
                for (net.minecraft.util.IChatComponent reply : replies.getAllValues()) {
                    net.minecraft.util.ChatComponentTranslation message = (net.minecraft.util.ChatComponentTranslation) reply;
                    if (message.getKey().equals("cmd.backup_prune_limit")) warned = true;
                    if (message.getKey().startsWith("cmd.backup_prune_summary")) {
                        summarized = true;
                        assertEquals(
                                serverutils.lib.util.FileUtils
                                        .getSizeString(policy.equals("forever:all") ? 6 * gb : 5 * gb),
                                message.getFormatArgs()[1]);
                    }
                    if (message.getKey().equals("cmd.backup_prune_allowance")) {
                        assertEquals(
                                serverutils.lib.util.FileUtils
                                        .getSizeString(policy.equals("forever:all") ? 0L : 2 * gb),
                                message.getFormatArgs()[0]);
                    }
                }
                assertTrue(summarized);
                assertEquals(policy.equals("1d:all"), warned);
            }
        } finally {
            serverutils.ServerUtilitiesConfig.backups.max_folder_size = previousLimit;
        }
    }

    @Test
    public void preservesFutureAndUnrecognizedArchives() {
        BackupRetention.Archive unknown = new BackupRetention.Archive(
                new File("unknown"),
                null,
                0,
                10,
                false,
                "Unrecognized/unreadable archive");
        BackupRetention.Archive future = archive("future", "a", MONDAY + DAY, false);
        BackupRetention.Archive latest = archive("latest", "a", MONDAY - 40 * DAY, false);
        BackupRetention.Plan plan = select(Arrays.asList(unknown, future, latest), MONDAY, 1, true, "1h:all");
        assertTrue(plan.delete.isEmpty());
        assertEquals(30, plan.remainingSize);
        assertEquals(10, plan.remainingRotationSize);
    }

    private Path zip(String name, String comment, String entryName, byte[] contents) throws Exception {
        return zip(name, comment, entryName, contents, true);
    }

    private Path zip(String name, String comment, String entryName, byte[] contents, boolean worldMetadata)
            throws Exception {
        Path file = temporary.getRoot().toPath().resolve(name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            if (comment != null) zip.setComment(comment);
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(contents);
            if (comment != null && worldMetadata) {
                zip.putNextEntry(new ZipEntry("saves/" + comment + "/level.dat"));
                zip.write(1);
            }
        }
        return file;
    }

    @Test
    public void unusableArchivesCannotDisplaceTheLastWorldBackup() throws Exception {
        Path valid = zip("valid.zip", "world", ICompress.BACKUP_METADATA_ENTRY, metadata(MONDAY, false));
        List<Path> unusable = Arrays.asList(
                zip(
                        "metadata-only.zip",
                        "world",
                        ICompress.BACKUP_METADATA_ENTRY,
                        metadata(MONDAY + 2 * DAY, false),
                        false),
                zip("comment-only.zip", "world", "unrelated.txt", new byte[] { 1 }, false),
                zip("wrong-world.zip", "world", "saves/other/level.dat", new byte[] { 1 }, false),
                zip("directory-marker.zip", "world", "saves/world/level.dat/", new byte[0], false),
                zip("empty-marker.zip", "world", "saves/world/level.dat", new byte[0], false),
                zip("two-layouts.zip", "world", "world/level.dat", new byte[] { 1 }));
        BackupRetention.Plan plan = BackupRetention
                .plan(temporary.getRoot(), new String[] { "1h:all" }, MONDAY + 2 * DAY, true, 1);
        assertTrue(plan.keep.get(valid.toFile()).contains("Latest backup for world"));
        for (Path path : unusable) {
            assertTrue(path.toString(), plan.keep.get(path.toFile()).contains("Unrecognized/unreadable"));
            assertFalse(plan.keep.get(path.toFile()).contains("Latest backup for world"));
        }
        assertTrue(plan.delete.isEmpty());
    }

    @Test
    public void supportsLevelDatOldInBothLayoutsAndCancellation() throws Exception {
        for (String prefix : new String[] { "world/", "saves/world/" }) {
            Path path = zip(
                    prefix.startsWith("saves") ? "singleplayer.zip" : "dedicated.zip",
                    "world",
                    prefix + "level.dat_old",
                    new byte[] { 1 },
                    false);
            BackupRetention.Plan plan = BackupRetention
                    .plan(temporary.getRoot(), new String[] { "forever:all" }, System.currentTimeMillis(), true, 0);
            assertFalse(plan.keep.get(path.toFile()).contains("Unrecognized/unreadable"));
        }
        Thread.currentThread().interrupt();
        try {
            assertThrows(
                    java.io.InterruptedIOException.class,
                    () -> BackupRetention.plan(temporary.getRoot(), new String[] { "forever:all" }, MONDAY, true, 0));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void nestedWorldRecognitionPreservesUnsafeAndAmbiguousArchives() throws Exception {
        List<Path> rejected = new ArrayList<>();
        for (String prefix : new String[] { "../world/", "parent/../world/", "/world/", "C:/world/", "parent//world/",
                "parent/./world/" }) {
            rejected.add(
                    zip("unsafe-" + rejected.size() + ".zip", "world", prefix + "level.dat", new byte[] { 1 }, false));
        }
        Path ambiguous = temporary.getRoot().toPath().resolve("ambiguous.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(ambiguous))) {
            zip.setComment("world");
            for (String prefix : new String[] { "worlds/world/", "other/world/" }) {
                zip.putNextEntry(new ZipEntry(prefix + "level.dat"));
                zip.write(1);
            }
        }
        rejected.add(ambiguous);
        Path valid = zip("nested-old.zip", "world", "worlds/world/level.dat_old", new byte[] { 1 }, false);
        BackupRetention.Plan plan = BackupRetention
                .plan(temporary.getRoot(), new String[] { "forever:all" }, System.currentTimeMillis(), true, 1);
        assertTrue(plan.keep.get(valid.toFile()).contains("Latest backup for world"));
        for (Path file : rejected) {
            assertTrue(file.toString(), plan.keep.get(file.toFile()).contains("Unrecognized/unreadable"));
            assertThrows(java.io.IOException.class, () -> BackupRetention.isCustomWorldArchive(file.toFile()));
        }
        assertTrue(plan.delete.isEmpty());
    }

    private static byte[] metadata(long timestamp, boolean custom) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("version", "1");
        properties.setProperty("worldId", WORLD);
        properties.setProperty("createdAt", Long.toString(timestamp));
        properties.setProperty("customName", Boolean.toString(custom));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        properties.store(bytes, null);
        return bytes.toByteArray();
    }

    @Test
    public void metadataSurvivesCopiesAndIdentifiesTimestampLookingCustomNames() throws Exception {
        Path named = zip("2026-10-05-00-00-00.zip", "world", ICompress.BACKUP_METADATA_ENTRY, metadata(MONDAY, true));
        Path newest = zip(
                "newest.zip",
                "renamed-world",
                ICompress.BACKUP_METADATA_ENTRY,
                metadata(MONDAY + 2 * DAY, false));
        Files.setLastModifiedTime(named, FileTime.fromMillis(MONDAY + 3 * DAY));
        BackupRetention.Plan protectedPlan = BackupRetention
                .plan(temporary.getRoot(), new String[] { "1h:all" }, MONDAY + 2 * DAY, false, 0);
        assertTrue(protectedPlan.keep.get(named.toFile()).contains("Protected custom"));
        BackupRetention.Plan plan = BackupRetention
                .plan(temporary.getRoot(), new String[] { "1h:all" }, MONDAY + 2 * DAY, true, 0);
        assertTrue(plan.delete.containsKey(named.toFile()));
        assertTrue(plan.keep.containsKey(newest.toFile()));
    }

    @Test
    public void readsLegacyUuidAlongsideNewMetadataAndLeavesOtherFilesAlone() throws Exception {
        NBTTagCompound universe = new NBTTagCompound();
        // Universe writes UUIDs without hyphens, while metadata uses the standard form.
        universe.setString("UUID", serverutils.lib.util.StringUtils.fromUUID(java.util.UUID.fromString(WORLD)));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(universe, bytes);
        Path legacy = zip(
                "before-upgrade.zip",
                "world",
                "saves/world/serverutilities/universe.dat",
                bytes.toByteArray());
        Files.setLastModifiedTime(legacy, FileTime.fromMillis(MONDAY));
        Path modern = zip(
                "after-upgrade.zip",
                "world",
                ICompress.BACKUP_METADATA_ENTRY,
                metadata(MONDAY + 2 * DAY, false));
        Path unrelated = Files.write(temporary.getRoot().toPath().resolve("readme.txt"), new byte[] { 1 });
        Path incomplete = Files.write(temporary.getRoot().toPath().resolve(".su-save-123.tmp"), new byte[] { 1 });
        Path corrupt = Files.write(temporary.getRoot().toPath().resolve("corrupt.zip"), new byte[] { 1 });
        Path unknown = zip("unknown.zip", null, "file", new byte[] { 1 });
        NBTTagCompound invalid = new NBTTagCompound();
        invalid.setString("UUID", "not-a-uuid");
        ByteArrayOutputStream invalidBytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(invalid, invalidBytes);
        Path invalidUuid = zip(
                "invalid-uuid.zip",
                "world",
                "saves/world/serverutilities/universe.dat",
                invalidBytes.toByteArray());
        Files.setLastModifiedTime(invalidUuid, FileTime.fromMillis(MONDAY));
        BackupRetention.Plan plan = BackupRetention
                .plan(temporary.getRoot(), new String[] { "1h:all" }, MONDAY + 2 * DAY, true, 0);
        assertTrue("Same world as the newer metadata backup", plan.delete.containsKey(legacy.toFile()));
        assertTrue(plan.isPreserved(invalidUuid.toFile()));
        assertTrue(plan.keep.containsKey(modern.toFile()));
        assertTrue(plan.keep.containsKey(corrupt.toFile()));
        assertTrue(plan.keep.containsKey(unknown.toFile()));
        assertFalse(plan.keep.containsKey(unrelated.toFile()));
        assertFalse(plan.keep.containsKey(incomplete.toFile()));
        assertTrue(Files.exists(legacy));
    }

    @Test
    public void malformedMetadataAndPolicyNeverBecomeDeletionCandidates() throws Exception {
        Path bad = zip(
                "bad.zip",
                "world",
                ICompress.BACKUP_METADATA_ENTRY,
                "version=99\n".getBytes(StandardCharsets.UTF_8));
        BackupRetention.Plan plan = BackupRetention
                .plan(temporary.getRoot(), new String[] { "1h:all" }, MONDAY, true, 1);
        assertTrue(plan.keep.containsKey(bad.toFile()));
        assertTrue(plan.delete.isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () -> BackupRetention.plan(temporary.getRoot(), new String[] { "1h:all", "bad" }, MONDAY, true, 0));
        assertTrue(Files.exists(bad));
    }

    @Test
    public void corruptedAndOversizedMetadataArePreserved() throws Exception {
        Path corrupted = temporary.getRoot().toPath().resolve("corrupted-metadata.zip");
        byte[] contents = metadata(MONDAY, false);
        CRC32 checksum = new CRC32();
        checksum.update(contents);
        ZipEntry entry = new ZipEntry(ICompress.BACKUP_METADATA_ENTRY);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(contents.length);
        entry.setCrc(checksum.getValue());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(corrupted))) {
            zip.setComment("world");
            zip.putNextEntry(entry);
            zip.write(contents);
            zip.putNextEntry(new ZipEntry("saves/world/level.dat"));
            zip.write(1);
        }
        byte[] archiveBytes = Files.readAllBytes(corrupted);
        int index = new String(archiveBytes, StandardCharsets.ISO_8859_1).indexOf("worldId=") + "worldId=".length();
        archiveBytes[index] = '2';
        Files.write(corrupted, archiveBytes);
        Path oversized = zip("oversized-metadata.zip", "world", ICompress.BACKUP_METADATA_ENTRY, new byte[16_385]);
        BackupRetention.Plan plan = BackupRetention
                .plan(temporary.getRoot(), new String[] { "1h:all" }, MONDAY, true, 1);
        assertTrue(plan.delete.isEmpty());
        assertTrue(plan.keep.get(corrupted.toFile()).contains("checksum"));
        assertTrue(plan.keep.get(oversized.toFile()).contains("too large"));
        assertThrows(java.io.IOException.class, () -> BackupRetention.isCustomWorldArchive(corrupted.toFile()));
        assertThrows(java.io.IOException.class, () -> BackupRetention.isCustomWorldArchive(oversized.toFile()));
    }

    @Test
    public void writesMetadataWithBothCompressorsIncludingUncompressedArchives() throws Exception {
        int previousLevel = serverutils.ServerUtilitiesConfig.backups.compression_level;
        try {
            serverutils.ServerUtilitiesConfig.backups.compression_level = 0;
            for (ICompress compressor : Arrays.asList(new LegacyCompressor(), new CommonsCompressor())) {
                Path path = temporary.newFile().toPath();
                try (ICompress output = compressor) {
                    output.createOutputStream(path.toFile());
                    BackupRetention.writeMetadata(output, WORLD, MONDAY, true);
                }
                try (ZipFile zip = new ZipFile(path.toFile())) {
                    Properties properties = new Properties();
                    properties.load(zip.getInputStream(zip.getEntry(ICompress.BACKUP_METADATA_ENTRY)));
                    assertEquals(WORLD, properties.getProperty("worldId"));
                    assertEquals(Long.toString(MONDAY), properties.getProperty("createdAt"));
                    assertEquals("true", properties.getProperty("customName"));
                }
            }
        } finally {
            serverutils.ServerUtilitiesConfig.backups.compression_level = previousLevel;
        }
    }
}
