package serverutils.pregenerator.filemanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import net.minecraft.server.MinecraftServer;

import serverutils.lib.util.misc.PregeneratorCommandInfo;
import serverutils.pregenerator.filemanager.readwriters.FileReadWriter;
import serverutils.pregenerator.filemanager.readwriters.SafeFileReadWriter;

public class PregeneratorFileManager {

    private final FileReadWriter commandReadWriter;
    private final SafeFileReadWriter iterationReadWriter;
    public static final String COMMAND_FOLDER = "pregenerationFiles";
    public static final String COMMAND_FILE = "fileCommand";
    public static final String COMMAND_ITERATION = "fileIteration";

    public PregeneratorFileManager(MinecraftServer server, double xLoc, double zLoc, int radius, int dimensionID)
            throws IOException {
        Path temporaryFileSaveFolder = Paths.get("saves").resolve(getWorldFolderPath(server).resolve(COMMAND_FOLDER));
        if (!Files.exists(temporaryFileSaveFolder)) {
            Files.createDirectories(temporaryFileSaveFolder);
        }
        this.iterationReadWriter = new SafeFileReadWriter(temporaryFileSaveFolder.resolve(COMMAND_ITERATION), 100);
        this.commandReadWriter = new FileReadWriter(temporaryFileSaveFolder.resolve(COMMAND_FILE));

        commandReadWriter.clearFile();
        commandReadWriter.writeDouble(xLoc);
        commandReadWriter.writeDouble(zLoc);
        commandReadWriter.writeInt(radius);
        commandReadWriter.writeInt(dimensionID);
        commandReadWriter.close();
    }

    // Constructor to load the information from file
    public PregeneratorFileManager(MinecraftServer server) throws IOException {
        Path temporaryFileSaveFolder = Paths.get("saves").resolve(getWorldFolderPath(server).resolve(COMMAND_FOLDER));
        if (!Files.exists(temporaryFileSaveFolder)) {
            Files.createDirectories(temporaryFileSaveFolder);
        }
        this.iterationReadWriter = new SafeFileReadWriter(temporaryFileSaveFolder.resolve(COMMAND_ITERATION), 100);
        this.commandReadWriter = new FileReadWriter(temporaryFileSaveFolder.resolve(COMMAND_FILE));
    }

    public Optional<PregeneratorCommandInfo> getCommandInfo() {
        try {
            commandReadWriter.openForReading();
            iterationReadWriter.openForReading();
            return Optional.of(
                    new PregeneratorCommandInfo(
                            commandReadWriter.readDouble(),
                            commandReadWriter.readDouble(),
                            commandReadWriter.readInt(),
                            commandReadWriter.readInt(),
                            iterationReadWriter.readInt()));
        } catch (IOException ignored) {} // Ignoring this because often there's just nothing in the file if you're
                                         // loading a world
        return Optional.empty();
    }

    public void saveIteration(int iteration) {
        try {
            iterationReadWriter.writeAndCommitIntAfterIterations(iteration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeAndRemoveAllFiles() {
        try {
            iterationReadWriter.close();
            iterationReadWriter.deleteFile();
            commandReadWriter.close();
            commandReadWriter.deleteFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeAllFiles() {
        try {
            iterationReadWriter.close();
            commandReadWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isReady() {
        return true;
    }

    private Path getWorldFolderPath(MinecraftServer server) {
        return Paths.get(server.getFolderName());
    }
}
