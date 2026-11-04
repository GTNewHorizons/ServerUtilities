package serverutils.task.backup.repository;

import java.util.Arrays;

/**
 * Chunk id to pack location, as parallel arrays with open addressing. A large world has around a million chunks, which
 * would cost several times more memory as boxed map entries.
 */
final class ChunkIndex {

    static final int ID_LENGTH = 32;

    private byte[] ids;
    // Pack ids start at 1, so 0 marks a free slot.
    private int[] packs;
    private int[] offsets;
    private int[] storedLengths;
    private int[] rawLengths;
    private int size;

    ChunkIndex() {
        this(1024);
    }

    ChunkIndex(int expected) {
        int capacity = Integer.highestOneBit(Math.max(16, (int) Math.min(1 << 30, expected * 4L / 3 + 1)) - 1) << 1;
        allocate(capacity);
    }

    private void allocate(int capacity) {
        ids = new byte[capacity * ID_LENGTH];
        packs = new int[capacity];
        offsets = new int[capacity];
        storedLengths = new int[capacity];
        rawLengths = new int[capacity];
    }

    int size() {
        return size;
    }

    /** Slots are stable until the next {@link #put} of a new id. */
    int capacity() {
        return packs.length;
    }

    boolean isUsed(int slot) {
        return packs[slot] != 0;
    }

    int find(byte[] id, int offset) {
        int mask = packs.length - 1;
        for (int slot = hash(id, offset) & mask;; slot = (slot + 1) & mask) {
            if (packs[slot] == 0) return -1;
            if (matches(slot, id, offset)) return slot;
        }
    }

    boolean contains(byte[] id) {
        return find(id, 0) >= 0;
    }

    /** Adds the id or moves it to the given location, and returns its slot. */
    int put(byte[] id, int offset, int pack, int packOffset, int storedLength, int rawLength) {
        if (pack <= 0) throw new IllegalArgumentException("Invalid pack id " + pack);
        if ((size + 1) * 4L > packs.length * 3L) grow();
        int mask = packs.length - 1;
        int slot = hash(id, offset) & mask;
        while (packs[slot] != 0 && !matches(slot, id, offset)) slot = (slot + 1) & mask;
        if (packs[slot] == 0) {
            System.arraycopy(id, offset, ids, slot * ID_LENGTH, ID_LENGTH);
            size++;
        }
        packs[slot] = pack;
        offsets[slot] = packOffset;
        storedLengths[slot] = storedLength;
        rawLengths[slot] = rawLength;
        return slot;
    }

    int pack(int slot) {
        return packs[slot];
    }

    int offset(int slot) {
        return offsets[slot];
    }

    int storedLength(int slot) {
        return storedLengths[slot];
    }

    int rawLength(int slot) {
        return rawLengths[slot];
    }

    /** Bytes the chunk occupies in its pack, including the record header. */
    long recordLength(int slot) {
        return PackFile.RECORD_HEADER + (long) storedLengths[slot];
    }

    void copyId(int slot, byte[] destination, int offset) {
        System.arraycopy(ids, slot * ID_LENGTH, destination, offset, ID_LENGTH);
    }

    byte[] id(int slot) {
        return Arrays.copyOfRange(ids, slot * ID_LENGTH, (slot + 1) * ID_LENGTH);
    }

    private boolean matches(int slot, byte[] id, int offset) {
        int base = slot * ID_LENGTH;
        for (int i = 0; i < ID_LENGTH; i++) {
            if (ids[base + i] != id[offset + i]) return false;
        }
        return true;
    }

    private static int hash(byte[] id, int offset) {
        // Ids are SHA-256 digests, so any four bytes are already uniformly distributed.
        return (id[offset] & 0xff) << 24 | (id[offset + 1] & 0xff) << 16
                | (id[offset + 2] & 0xff) << 8
                | (id[offset + 3] & 0xff);
    }

    private void grow() {
        if (packs.length >= 1 << 30) throw new IllegalStateException("Chunk index is full");
        byte[] oldIds = ids;
        int[] oldPacks = packs, oldOffsets = offsets, oldStored = storedLengths, oldRaw = rawLengths;
        allocate(packs.length * 2);
        size = 0;
        for (int slot = 0; slot < oldPacks.length; slot++) {
            if (oldPacks[slot] != 0) {
                put(oldIds, slot * ID_LENGTH, oldPacks[slot], oldOffsets[slot], oldStored[slot], oldRaw[slot]);
            }
        }
    }
}
