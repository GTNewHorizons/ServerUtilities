package serverutils.lib.util.backup;

public class SnapshotException extends RuntimeException {
    public SnapshotException(String message) {
        super(message);
    }

    public SnapshotException(String message, Throwable cause) {
        super(message, cause);
    }
}
