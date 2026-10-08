package serverutils.lib.util.backup;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

public class SnapshotJson {

    private static final Gson GSON = new Gson();

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
        }
    }

    // Atomic writing for JSON
    public static void write(File file, SnapshotJson snapshot) throws IOException {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileWriter fileWriter = new FileWriter(temp)) {
            GSON.toJson(snapshot, fileWriter);
        }
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static SnapshotJson read(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, SnapshotJson.class);
        }
    }
}
