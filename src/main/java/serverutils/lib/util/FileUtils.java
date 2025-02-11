package serverutils.lib.util;

import static serverutils.lib.util.FileUtils.SizeUnit.GB;
import static serverutils.lib.util.FileUtils.SizeUnit.KB;
import static serverutils.lib.util.FileUtils.SizeUnit.MB;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(newFile(file)), StandardCharsets.UTF_8);
        BufferedWriter br = new BufferedWriter(fw);

        for (String s : list) {
            br.write(s);
            br.write('\n');
        }

        br.close();
        fw.close();
    }

    public static void save(File file, String string) throws Exception {
        OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(newFile(file)), StandardCharsets.UTF_8);
        BufferedWriter br = new BufferedWriter(fw);
        br.write(string);
        br.close();
        fw.close();
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
}
