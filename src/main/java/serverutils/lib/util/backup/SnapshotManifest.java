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

import com.google.gson.Gson;

public class SnapshotManifest {

    private static final Gson GSON = new Gson();
    private String name;

    public String createdAt;
    public String completedAt;
    public String minecraftVersion;
    public String modVersion;
    public List<Dimension> dimensions = new ArrayList<>();
    public Map<String, String> files = new HashMap<>();

    public static class Dimension {

        public int id;
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

    public String getName() {
        return name;
    }

    public ZonedDateTime getCreatedAt() {
        return ZonedDateTime.parse(createdAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public ZonedDateTime getCompletedAt() {
        return ZonedDateTime.parse(completedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
