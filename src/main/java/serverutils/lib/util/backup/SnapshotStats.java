package serverutils.lib.util.backup;

public class SnapshotStats {

    public long durationMillis;
    public int newChunks;
    public long newBytes;

    /** Chunks that were read but already stored (e.g. identical to another chunk), and the bytes not rewritten. */
    public int dedupedChunks;
    public long dedupedBytes;

    public int unmodifiedChunks;
    public int unmodifiedRegions;

    /** Counts for the non-region files of the world. */
    public int newFiles;
    public int unmodifiedMiscFiles;

    public void unmodified(SnapshotEntry entry) {
        switch (entry.kind()) {
            case REGION -> unmodifiedRegions++;
            case MISC -> unmodifiedMiscFiles++;
        }
    }
}
