package serverutils.pregenerator.filemanager.readwriters;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

// TODO : MAKE THIS BOUNCE BETWEEN 2 FILES
public class SafeFileReadWriter extends FileReadWriter {

    private final Path tempFile;
    private RandomAccessFile randomAccessFileTemp;
    private boolean randomAccessFileTempIsClosed = false;
    private final int iterationsBetweenWrites;
    private int writeIteration = 0;

    public SafeFileReadWriter(Path path, int iterationsBetweenWrites) throws IOException {
        super(path);
        this.iterationsBetweenWrites = iterationsBetweenWrites;
        this.tempFile = Paths.get(path + ".tmp");
        randomAccessFileTemp = new RandomAccessFile(this.tempFile.toFile(), "rw");
    }

    public void writeAndCommitIntAfterIterations(int value) throws IOException {
        if (writeIteration >= iterationsBetweenWrites) {
            writeIteration = 0;
            this.writeInt(value);
            this.commit();
        } else {
            writeIteration++;
        }
    }

    @Override
    public void writeInt(int value) throws IOException {
        if (this.randomAccessFileTempIsClosed) {
            this.openForWriting();
        }
        randomAccessFileTemp.seek(0);
        randomAccessFileTemp.writeInt(value);
        randomAccessFileTemp.getChannel().force(true);
    }

    public void commit() throws IOException {
        randomAccessFileTemp.close();
        randomAccessFileTempIsClosed = true;
        randomAccessFile.close();
        randomAccessFileIsClosed = true;
        Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        this.openForWriting();
    }

    @Override
    public void clearFile() throws IOException {
        randomAccessFile.setLength(0);
        randomAccessFileTemp.setLength(0);
    }

    @Override
    public void close() throws IOException {
        randomAccessFileIsClosed = true;
        randomAccessFile.close();

        randomAccessFileTempIsClosed = true;
        randomAccessFileTemp.close();
        Files.deleteIfExists(tempFile);
    }

    @Override
    public void openForWriting() throws IOException {
        if (randomAccessFileTemp == null || !tempFile.toFile().exists() || randomAccessFileTempIsClosed) {
            randomAccessFileTempIsClosed = false;
            randomAccessFileTemp = new RandomAccessFile(tempFile.toFile(), "rw");
        }
    }
}
