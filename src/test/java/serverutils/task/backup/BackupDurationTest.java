package serverutils.task.backup;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.gtnewhorizon.gtnhlib.config.ConfigFieldParser;

import serverutils.ServerUtilitiesConfig;

public class BackupDurationTest {

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void parsesTheSamePositiveUnitsUsedByRetention() {
        String[] values = { "1s", "30m", "6h", "1d", "2w" };
        long[] expected = { 1000, 1800000, 21600000, 86400000, 1209600000 };
        for (int i = 0; i < values.length; i++) assertEquals(expected[i], BackupDuration.parse(values[i]));
        for (String invalid : new String[] { "0s", "-1h", "1.5h", "1month", "all", "forever", "30", "1H", "",
                "9223372036854776s" }) {
            assertThrows(invalid, IllegalArgumentException.class, () -> BackupDuration.parse(invalid));
        }
    }

    @Test
    public void migratesHoursByRoundingUpSecondsAndChoosingAnExactUnit() {
        String[][] values = { { "0.5", "30m" }, { "1.5", "90m" }, { "24", "1d" }, { "168", "1w" }, { "0.0834", "301s" },
                { "0.1", "6m" }, { "0", "1s" }, { "0.0", "1s" }, { "-0.0", "1s" }, { "1e-7", "1s" }, { " 6.0 ", "6h" },
                { "5m", "5m" }, { " 60s ", "60s" } };
        for (String[] value : values) {
            String normalized = BackupDuration.normalizeTimer(value[0]);
            assertEquals(value[0], value[1], normalized);
            assertEquals("Migration is idempotent", normalized, BackupDuration.normalizeTimer(normalized));
        }
    }

    @Test
    public void rejectsInvalidAndOverflowingTimersInsteadOfGuessingAnInterval() {
        for (String invalid : new String[] { "-0.5", "NaN", "Infinity", "0s", "0m", "1.5h", "all", "forever", "",
                "garbage", "1h 30m", "1e100", "9223372036854776s", "2562047788016", null }) {
            assertThrows(invalid, IllegalArgumentException.class, () -> BackupDuration.normalizeTimer(invalid));
        }
    }

    @Test
    public void migratesRealForgePropertiesWithoutLosingTheirGuiSettingsOrOtherValues() throws Exception {
        java.lang.reflect.Field minecraftHome = cpw.mods.fml.relauncher.FMLInjectionData.class
                .getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        Object previousHome = minecraftHome.get(null);
        String previous = ServerUtilitiesConfig.backups.backup_timer;
        try {
            minecraftHome.set(null, temporary.getRoot());
            String[][] values = { { "D", "0.5", "30m" }, { "D", "0.0", "1s" }, { "S", "0.0834", "301s" },
                    { "S", "5m", "5m" }, { "D", "-1.0", "-1.0" }, { "S", "invalid", "invalid" } };
            for (int i = 0; i < values.length; i++) {
                String[] value = values[i];
                File file = temporary.newFile("timer-" + i + ".cfg");
                Files.write(
                        file.toPath(),
                        ("backups {\n" + value[0] + ":backup_timer=" + value[1] + "\nI:backups_to_keep=7\n}\n")
                                .getBytes(StandardCharsets.UTF_8));
                Configuration config = new Configuration(file);
                config.load();
                ConfigFieldParser.loadField(
                        ServerUtilitiesConfig.backups,
                        ServerUtilitiesConfig.Backups.class.getDeclaredField("backup_timer"),
                        config,
                        "backups",
                        "serverutilities.backups.backup_timer");
                assertEquals(value[1], ServerUtilitiesConfig.backups.backup_timer);
                config.getCategory("backups").get("backup_timer").setRequiresWorldRestart(true);
                ServerUtilitiesConfig.migrateBackupTimer(config);
                assertEquals(value[2], ServerUtilitiesConfig.backups.backup_timer);
                Property migrated = config.getCategory("backups").get("backup_timer");
                assertEquals(Property.Type.STRING, migrated.getType());
                assertEquals("30m", migrated.getDefault());
                assertEquals("serverutilities.backups.backup_timer", migrated.getLanguageKey());
                assertTrue(migrated.requiresWorldRestart());
                assertTrue(migrated.comment.contains("minimum 1s"));
                String saved = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                ServerUtilitiesConfig.migrateBackupTimer(config);
                assertEquals(saved, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
                Configuration reloaded = new Configuration(file);
                reloaded.load();
                assertEquals(Property.Type.STRING, reloaded.getCategory("backups").get("backup_timer").getType());
                assertEquals(value[2], reloaded.getCategory("backups").get("backup_timer").getString());
                assertEquals(7, reloaded.getCategory("backups").get("backups_to_keep").getInt());
            }
        } finally {
            minecraftHome.set(null, previousHome);
            ServerUtilitiesConfig.backups.backup_timer = previous;
        }
    }
}
