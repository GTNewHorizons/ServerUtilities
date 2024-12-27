package serverutils.pregenerator.filemanager.readwriters;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileReadWriter {

    protected RandomAccessFile randomAccessFile;
    protected boolean randomAccessFileIsClosed = false;
    protected final Path filePath;

    public FileReadWriter(Path path) throws IOException {
        this.filePath = path;
        randomAccessFile = new RandomAccessFile(this.filePath.toFile(), "rw");
    }

    public void close() throws IOException {
        randomAccessFileIsClosed = true;
        randomAccessFile.close();
    }

    public void clearFile() throws IOException {
        randomAccessFile.setLength(0);
    }

    public void writeDouble(double value) throws IOException {
        randomAccessFile.writeDouble(value);
    }

    public void writeInt(int value) throws IOException {
        randomAccessFile.writeInt(value);
    }

    public void openForWriting() throws IOException {
        if (randomAccessFile == null || !filePath.toFile().exists() || randomAccessFileIsClosed) {
            randomAccessFileIsClosed = false;
            randomAccessFile = new RandomAccessFile(filePath.toFile(), "rw");
        }
    }

    public void openForReading() throws IOException {
        if (randomAccessFile == null || !filePath.toFile().exists() || randomAccessFileIsClosed) {
            randomAccessFileIsClosed = false;
            randomAccessFile = new RandomAccessFile(filePath.toFile(), "r");
        }
    }

    public int readInt() throws IOException {
        return randomAccessFile.readInt();
    }

    public double readDouble() throws IOException {
        return randomAccessFile.readDouble();
    }

    public void deleteFile() throws IOException {
        Files.deleteIfExists(filePath);
    }
}
