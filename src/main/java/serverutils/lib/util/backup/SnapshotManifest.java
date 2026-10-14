package serverutils.lib.util.backup;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;

import lombok.Getter;
import serverutils.task.backup.BackupEntry;

public class SnapshotManifest implements BackupEntry {

    private static final Gson GSON = new Gson();
    @Getter
    private transient String name;
    private transient ZonedDateTime createdAtTime;

    public String world;
    public String label;
    public String createdAt;
    public String completedAt;
    public String minecraftVersion;
    public String modVersion;
    public List<Dimension> dimensions = new ArrayList<>();

    @Override
    public String id() {
        return name;
    }

    @Override
    public @Nullable String world() {
        return world;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public long createdAt() {
        return getCreatedAt().toInstant().toEpochMilli();
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public boolean isCustom() {
        return false;
    }

    @Override
    public @Nullable String problem() {
        return null;
    }

    public static class Dimension {

        public String id;
        public Kinds kinds = new Kinds();

        public static class Kinds {

            public Map<String, ManifestDescriptor> regions = new HashMap<>();
            public Map<String, ManifestDescriptor> files = new HashMap<>();
        }
    }

    // Atomic writing for JSON
    public static void write(File file, SnapshotManifest snapshot) throws IOException {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileWriter fileWriter = new FileWriter(temp)) {
            GSON.toJson(snapshot, fileWriter);
        }
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static SnapshotManifest read(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            SnapshotManifest json = GSON.fromJson(reader, SnapshotManifest.class);
            json.name = file.getName();
            return json;
        }
    }

    public ZonedDateTime getCreatedAt() {
        if (createdAtTime != null) return createdAtTime;
        createdAtTime = ZonedDateTime.parse(createdAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return createdAtTime;
    }

    public ZonedDateTime getCompletedAt() {
        return ZonedDateTime.parse(completedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    public String toString() {
        return BackupEntry.toString(this);
    }
}
