package serverutils.lib.util.compression;

import static serverutils.ServerUtilitiesConfig.backups;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Enumeration;
import java.util.zip.ZipEntry;

import javax.annotation.Nullable;

import net.minecraftforge.common.DimensionManager;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;

import serverutils.lib.util.FileUtils;

public class CommonsCompressor implements ICompress {

    private ArchiveOutputStream output;

    @Override
    public void createOutputStream(File file) throws IOException {
        ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(file);
        if (backups.compression_level == 0) {
            zaos.setMethod(ZipEntry.STORED);
        } else {
            zaos.setLevel(backups.compression_level);
        }

        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        if (worldDir != null) {
            zaos.setComment(worldDir.getName());
        }

        output = zaos;
    }

    @Override
    public void addFileToArchive(File file, String name) throws IOException {
        ArchiveEntry entry = output.createArchiveEntry(file, name);
        output.putArchiveEntry(entry);
        try (FileInputStream fis = new FileInputStream(file)) {
            IOUtils.copy(fis, output);
        }
        output.closeArchiveEntry();
    }

    private static boolean shouldExtract(File file, boolean includeGlobal) {
        if (includeGlobal) {
            return true;
        }
        for (String pattern : backups.additional_backup_files) {
            if (pattern.contains("$WORLDNAME")) {
                continue;
            }
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            if (matcher.matches(file.toPath())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isOldBackup(File archive) throws IOException {
        try (ZipFile zip = new ZipFile(archive)) {
            boolean isOldBackup = true;
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (entry.getName().replace('\\', '/').startsWith("saves/")) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void extractArchive(File archive, boolean includeGlobal, boolean isOldBackup) throws IOException {

        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            String prefix = isOldBackup ? "saves/" : "";
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();

                File file = new File(prefix + entry.getName());
                if (shouldExtract(file, includeGlobal)) {
                    file = FileUtils.newFile(file);
                    InputStream in = zip.getInputStream(entry);
                    OutputStream out = new FileOutputStream(file);
                    IOUtils.copy(in, out);

                    in.close();
                    out.close();
                }
            }
        }
    }

    @Override
    public @Nullable String getWorldName(File file) throws IOException {
        if (file.isDirectory() || !file.getName().endsWith(".zip")) return null;
        // uses native zip file implementation because reading the
        // comment from a commons compress ZipFile is significantly slower
        try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(file)) {
            return zipFile.getComment();
        }
    }

    @Override
    public void close() throws Exception {
        if (output != null) {
            output.close();
        }
    }
}
