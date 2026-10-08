package serverutils.lib.util.backup;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static void write(File file, SnapshotJson snapshot) throws IOException {
        try (FileWriter fileWriter = new FileWriter(file)) {
            GSON.toJson(snapshot, fileWriter);
        }
    }

    public static SnapshotJson read(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return GSON.fromJson(reader, SnapshotJson.class);
        }
    }
}
