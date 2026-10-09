package serverutils.lib.util.backup;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public final class SHAHash {

    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    });

    private byte[] bytes;
    private String string;

    public SHAHash(@Nullable byte[] bytes, @Nullable String string) {
        this.bytes = bytes;
        this.string = string;

        if (bytes == null && string == null) {
            throw new IllegalArgumentException("Either bytes or string must be provided");
        }
    }

    public SHAHash(byte[] hashBytes) {
        this(hashBytes, null);
    }

    public SHAHash(String string) {
        this(null, string);
    }

    public static SHAHash compute(int compressionType, byte[] data) {
        MessageDigest md = SHA256.get();
        md.reset();
        md.update((byte) compressionType);
        md.update(data);
        return new SHAHash(md.digest());
    }

    public static SHAHash compute(byte[] data) {
        MessageDigest md = SHA256.get();
        md.reset();
        return new SHAHash(md.digest(data));
    }

    public byte[] getBytes() {
        if (bytes != null) return bytes;
        try {
            bytes = Hex.decodeHex(string.toCharArray());
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
        return bytes;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SHAHash hash)) return false;

        if (hash.string != null && string != null) {
            return hash.string.equals(string);
        }

        if (hash.bytes != null && bytes != null) {
            return Arrays.equals(hash.bytes, bytes);
        }

        return hash.toString().equals(toString());
    }

    @Override
    public String toString() {
        if (string == null) {
            string = Hex.encodeHexString(bytes);
        }
        return string;
    }
}
