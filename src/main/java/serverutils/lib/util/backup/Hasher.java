package serverutils.lib.util.backup;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hasher {

    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    });

    public static byte[] hashChunk(int compressionType, byte[] data) {
        MessageDigest md = SHA256.get();
        md.reset();
        md.update((byte) compressionType);
        md.update(data);
        return md.digest();
    }

    public static byte[] hash(byte[] data) {
        MessageDigest md = SHA256.get();
        md.reset();
        return md.digest(data);
    }
}
