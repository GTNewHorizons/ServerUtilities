package serverutils.lib.util.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.bsideup.jabel.Desugar;

public final class PackIndex {

    public static final String MAGIC = "MIDX";
    public static final String EXTENSION = ".idx";
    private static final int HASH_SIZE = 32;

    @Desugar
    public record Entry(SHAHash hash, long offset, int length, int compressionType) {}

    private PackIndex() {}

    public static void write(File file, List<Entry> entries) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeBytes(MAGIC);
            out.writeInt(entries.size());
            for (Entry entry : entries) {
                out.write(entry.hash.getBytes());
                out.writeLong(entry.offset());
                out.writeInt(entry.length());
                out.writeByte(entry.compressionType());
            }
        }
    }

    public static List<Entry> read(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            byte[] magic = new byte[MAGIC.length()];
            in.readFully(magic);
            if (!MAGIC.equals(new String(magic, StandardCharsets.US_ASCII))) {
                throw new IOException("Invalid pack index magic in " + file);
            }

            int count = in.readInt();
            List<Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                byte[] hash = new byte[HASH_SIZE];
                in.readFully(hash);
                entries.add(new Entry(new SHAHash(hash), in.readLong(), in.readInt(), in.readUnsignedByte()));
            }
            return entries;
        }
    }

    public static Map<File, List<Entry>> loadAllEntries(File packsDirectory) throws IOException {
        File[] indexes = packsDirectory.listFiles((dir, name) -> name.endsWith(EXTENSION));
        if (indexes == null) return Collections.emptyMap();
        Map<File, List<Entry>> result = new HashMap<>(indexes.length);

        for (File index : indexes) {
            result.put(index, read(index));
        }

        return result;
    }

    public static Set<SHAHash> loadKnownHashes(File packsDirectory) throws IOException {
        Set<SHAHash> hashes = new HashSet<>();

        for (List<Entry> entries : loadAllEntries(packsDirectory).values()) {
            for (Entry entry : entries) {
                hashes.add(entry.hash());
            }
        }

        return hashes;
    }
}
