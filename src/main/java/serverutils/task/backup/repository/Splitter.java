package serverutils.task.backup.repository;

import java.io.IOException;

/** Cuts a byte stream into chunks and hands each one to a {@link Chunker.Sink}. */
interface Splitter {

    void update(byte[] data, int offset, int count) throws IOException;

    /** Emits the remaining bytes as the last chunk. */
    void finish() throws IOException;
}
