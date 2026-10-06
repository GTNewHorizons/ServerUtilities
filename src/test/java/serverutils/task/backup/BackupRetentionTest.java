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
        BackupRetention.Plan plan = select(input, latest.created, 40, false, "1d:all", "forever:1w");
        assertEquals(Collections.singleton(finite.file), plan.delete.keySet());
        assertEquals("Folder size limit", plan.delete.get(finite.file));
        plan = select(input, latest.created, 1, false, "1d:all", "forever:1w");
        assertTrue(plan.keep.containsKey(weekly.file));
        assertTrue(plan.keep.containsKey(named.file));
        assertTrue(plan.keep.containsKey(latest.file));
        assertEquals(30, plan.remainingSize);
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
    }

    private Path zip(String name, String comment, String entryName, byte[] contents) throws Exception {
        Path file = temporary.getRoot().toPath().resolve(name);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            if (comment != null) zip.setComment(comment);
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(contents);
        }
        return file;
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
        universe.setString("UUID", WORLD);
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
        BackupRetention.Plan plan = BackupRetention
                .plan(temporary.getRoot(), new String[] { "1h:all" }, MONDAY + 2 * DAY, true, 0);
        assertTrue(plan.delete.containsKey(legacy.toFile()));
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
            zip.putNextEntry(entry);
            zip.write(contents);
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
