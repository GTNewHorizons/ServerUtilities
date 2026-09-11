package serverutils.lib.util;

import static serverutils.lib.util.FileUtils.SizeUnit.GB;
import static serverutils.lib.util.FileUtils.SizeUnit.KB;
import static serverutils.lib.util.FileUtils.SizeUnit.MB;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.storage.ThreadedFileIOBase;

public class FileUtils {

    public enum SizeUnit {

        B(1),
        KB(B.size * 1024),
        MB(KB.size * 1024),
        GB(MB.size * 1024);

        private final long size;

        SizeUnit(long size) {
            this.size = size;
        }

        public long getSize() {
            return size;
        }
    }

    public static File newFile(File file) {
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    public static void save(File file, Iterable<String> list) throws Exception {
        StringBuilder text = new StringBuilder();
        for (String line : list) text.append(line).append('\n');
        save(file, text.toString());
    }

    public static void save(File file, String string) throws Exception {
        writeAtomic(file, string.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeAtomic(File file, byte[] data) throws IOException {
        Path target = file.toPath().toAbsolutePath();
        Files.createDirectories(target.getParent());
        boolean posix = Files.getFileAttributeView(target.getParent(), PosixFileAttributeView.class) != null;
        Path temporary = posix
                ? Files.createTempFile(
                        target.getParent(),
                        ".su-save-",
                        ".tmp",
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-rw-rw-")))
                : Files.createTempFile(target.getParent(), ".su-save-", ".tmp");
        try {
            if (posix && Files.exists(target)) {
                Files.setPosixFilePermissions(temporary, Files.getPosixFilePermissions(target));
            }
            try (FileOutputStream output = new FileOutputStream(temporary.toFile())) {
                output.write(data);
                output.getFD().sync();
            }
            // Fail closed if atomic replacement is unavailable; keep the previous complete save.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void saveSafe(final File file, final Iterable<String> list) {
        ThreadedFileIOBase.threadedIOInstance.queueIO(() -> {
            try {
                save(file, list);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return false;
        });
    }

    public static void saveSafe(final File file, final String string) {
        ThreadedFileIOBase.threadedIOInstance.queueIO(() -> {
            try {
                save(file, string);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return false;
        });
    }

    public static List<File> listTree(File file) {
        List<File> l = new ArrayList<>();
        listTree0(l, file);
        return l;
    }

    public static void listTree0(List<File> list, File file) {
        if (file.isDirectory()) {
            File[] fl = file.listFiles();

            if (fl != null && fl.length > 0) {
                for (File aFl : fl) {
                    listTree0(list, aFl);
                }
            }
        } else if (file.isFile()) {
            list.add(file);
        }
    }

    public static long getSize(File file) {
        return getSize(file, SizeUnit.B);
    }

    public static long getSize(File file, SizeUnit sizeUnit) {
        long size = getSize0(file);
        if (size == 0L) return 0L;

        return switch (sizeUnit) {
            case KB -> size / KB.getSize();
            case MB -> size / MB.getSize();
            case GB -> size / GB.getSize();
            default -> size;
        };
    }

    private static long getSize0(File file) {
        if (!file.exists()) return 0L;
        long size = 0;

        if (file.isFile()) {
            size += file.length();
        } else if (file.isDirectory()) {
            File[] f1 = file.listFiles();
            if (f1 != null && f1.length > 0) {
                for (File aF1 : f1) {
                    size += getSize0(aF1);
                }
            }
        }

        return size;
    }

    public static String getSizeString(double b) {
        if (b >= GB.getSize()) {
            return String.format("%.1fGB", b / (double) GB.getSize());
        } else if (b >= MB.getSize()) {
            return String.format("%.1fMB", b / (double) MB.getSize());
        } else if (b >= KB.getSize()) {
            return String.format("%.1fKB", b / (double) KB.getSize());
        }

        return b + "B";
    }

    public static String getSizeString(File file) {
        return getSizeString(getSize(file));
    }

    public static void copyFile(File src, File dst) throws Exception {
        if (src.exists() && !src.equals(dst)) {
            if (src.isDirectory() && dst.isDirectory()) {
                for (File f : listTree(src)) {
                    File dst1 = new File(
                            dst.getAbsolutePath() + File.separatorChar
                                    + (f.getAbsolutePath().replace(src.getAbsolutePath(), "")));
                    copyFile(f, dst1);
                }
            } else {
                dst = newFile(dst);

                try (FileInputStream fis = new FileInputStream(src);
                        FileOutputStream fos = new FileOutputStream(dst);
                        FileChannel srcC = fis.getChannel();
                        FileChannel dstC = fos.getChannel()) {
                    dstC.transferFrom(srcC, 0L, srcC.size());
                }
            }
        }
    }

    public static boolean delete(File file) {
        if (!file.exists()) {
            return false;
        } else if (file.isFile()) {
            return file.delete();
        }

        String[] files = file.list();

        if (files != null) {
            for (String s : files) {
                delete(new File(file, s));
            }
        }

        return file.delete();
    }

    public static void deleteSafe(File file) {
        ThreadedFileIOBase.threadedIOInstance.queueIO(() -> {
            try {
                if (file.exists() && !delete(file)) {
                    System.err.println("Failed to safely delete " + file.getAbsolutePath());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return false;
        });
    }

    public static String getBaseName(File file) {
        if (file.isDirectory()) {
            return file.getName();
        } else {
            String name = file.getName();
            int index = name.lastIndexOf('.');
            return index == -1 ? name : name.substring(0, index);
        }
    }

    public static String getRelativePath(File file) {
        Path filePath = file.toPath().toAbsolutePath();
        return Paths.get("").toAbsolutePath().relativize(filePath).toString().replace('\\', '/');
    }

    public static String normalizeBackupPattern(String pattern) {
        pattern = pattern.replace('\\', '/');
        while (pattern.startsWith("./")) pattern = pattern.substring(2);
        while (pattern.contains("/./")) pattern = pattern.replace("/./", "/");
        if (pattern.endsWith("/.")) pattern = pattern.substring(0, pattern.length() - 2);
        return pattern;
    }

    public static boolean matchesBackupPath(Path path, String pattern) {
        pattern = normalizeBackupPattern(pattern);
        path = path.normalize();
        return FileSystems.getDefault().getPathMatcher("glob:" + pattern).matches(path)
                || (!pattern.contains("*") && path.startsWith(Paths.get(pattern).normalize()));
    }
}
