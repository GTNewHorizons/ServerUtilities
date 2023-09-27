package serverutils.lib.lib.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BackupUtils {

    public static final DecimalFormat smallDoubleFormatter = new DecimalFormat("#0.00");
    public static final int KB = 1024;
    public static final int MB = 1048576;
    public static final int GB = 1073741824;
    public static final double KB_D = 1024.0;
    public static final double MB_D = 1048576.0;
    public static final double GB_D = 1.073741824E9;
    public static final Comparator<File> fileComparator = new Comparator<File>() {

        public int compare(File o1, File o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    };
    public static final Comparator<File> deepFileComparator = new Comparator<File>() {

        public int compare(File o1, File o2) {
            return o1.getAbsolutePath().compareToIgnoreCase(o2.getAbsolutePath());
        }
    };

    public BackupUtils() {}

    public static File newFile(File f) {
        if (f != null && !f.exists()) {
            try {
                File pf = f.getParentFile();
                if (!pf.exists()) {
                    pf.mkdirs();
                }

                f.createNewFile();
                return f;
            } catch (Exception var2) {
                var2.printStackTrace();
                return f;
            }
        } else {
            return f;
        }
    }

    public static void save(File f, List<String> al) throws Exception {
        save(f, StringUtils.fromStringList(al));
    }

    public static void save(File f, String s) throws Exception {
        FileWriter fw = new FileWriter(newFile(f));
        BufferedWriter br = new BufferedWriter(fw);
        br.write(s);
        br.close();
        fw.close();
    }

    public static List<String> load(File f) throws Exception {
        return StringUtils.readStringList(new FileInputStream(f));
    }

    public static String loadAsText(File f) throws Exception {
        return StringUtils.readString(new FileInputStream(f));
    }

    public static boolean downloadFile(String url, File out) {
        try {
            URL website = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(out);
            fos.getChannel().transferFrom(rbc, 0L, Long.MAX_VALUE);
            fos.close();
            return true;
        } catch (Exception var5) {
            return false;
        }
    }

    public static List<File> listAll(File f) {
        ArrayList<File> l = new ArrayList();
        addAllFiles(l, f);
        return l;
    }

    private static void addAllFiles(ArrayList<File> l, File f) {
        if (f.isDirectory()) {
            File[] fl = f.listFiles();
            if (fl != null && fl.length > 0) {
                File[] var3 = fl;
                int var4 = fl.length;

                for (int var5 = 0; var5 < var4; ++var5) {
                    File aFl = var3[var5];
                    addAllFiles(l, aFl);
                }
            }
        } else if (f.isFile()) {
            l.add(f);
        }

    }

    public static long getSize(File f) {
        if (f != null && f.exists()) {
            if (f.isFile()) {
                return f.length();
            } else if (!f.isDirectory()) {
                return 0L;
            } else {
                long length = 0L;
                File[] f1 = f.listFiles();
                if (f1 != null && f1.length > 0) {
                    File[] var4 = f1;
                    int var5 = f1.length;

                    for (int var6 = 0; var6 < var5; ++var6) {
                        File aF1 = var4[var6];
                        length += getSize(aF1);
                    }
                }

                return length;
            }
        } else {
            return 0L;
        }
    }

    public static String getSizeS(double b) {
        if (b >= 1.073741824E9) {
            b /= 1.073741824E9;
            b = (double) ((long) (b * 10.0)) / 10.0;
            return b + "GB";
        } else if (b >= 1048576.0) {
            b /= 1048576.0;
            b = (double) ((long) (b * 10.0)) / 10.0;
            return b + "MB";
        } else if (b >= 1024.0) {
            b /= 1024.0;
            b = (double) ((long) (b * 10.0)) / 10.0;
            return b + "KB";
        } else {
            return b + "B";
        }
    }

    public static String getSizeS(File f) {
        return getSizeS((double) getSize(f));
    }

    public static Exception copyFile(File src, File dst) {
        if (src != null && dst != null && src.exists() && !src.equals(dst)) {
            if (src.isDirectory() && dst.isDirectory()) {
                Iterator var7 = listAll(src).iterator();

                Exception e;
                do {
                    if (!var7.hasNext()) {
                        return null;
                    }

                    File f = (File) var7.next();
                    File dst1 = new File(
                            dst.getAbsolutePath() + File.separatorChar
                                    + f.getAbsolutePath().replace(src.getAbsolutePath(), ""));
                    e = copyFile(f, dst1);
                } while (e == null);

                return e;
            } else {
                dst = newFile(dst);

                try {
                    FileChannel srcC = (new FileInputStream(src)).getChannel();
                    FileChannel dstC = (new FileOutputStream(dst)).getChannel();
                    dstC.transferFrom(srcC, 0L, srcC.size());
                    srcC.close();
                    dstC.close();
                    return null;
                } catch (Exception var6) {
                    return var6;
                }
            }
        } else {
            return null;
        }
    }

    public static boolean delete(File f) {
        if (f != null && f.exists()) {
            if (f.isFile()) {
                return f.delete();
            } else {
                String[] files = f.list();
                String[] var2 = files;
                int var3 = files.length;

                for (int var4 = 0; var4 < var3; ++var4) {
                    String file = var2[var4];
                    delete(new File(f, file));
                }

                return f.delete();
            }
        } else {
            return false;
        }
    }

    public static File getSourceDirectory(Class<?> c) {
        return new File(c.getProtectionDomain().getCodeSource().getLocation().getFile());
    }

    public static String getRawFileName(File f) {
        if (f != null && f.exists()) {
            if (f.isDirectory()) {
                return f.getName();
            } else if (f.isFile()) {
                String s = f.getName();
                return s.substring(0, s.lastIndexOf(46));
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    public static long millis() {
        return System.currentTimeMillis();
    }

    public static String toSmallDouble(double d) {
        return smallDoubleFormatter.format(d);
    }

    public static class Time implements Comparable<Time> {

        public final long millis;
        public final int seconds;
        public final int minutes;
        public final int hours;
        public final int day;
        public final int month;
        public final int year;

        private Time(Calendar c) {
            this.millis = c.getTimeInMillis();
            this.seconds = c.get(13);
            this.minutes = c.get(12);
            this.hours = c.get(11);
            this.day = c.get(5);
            this.month = c.get(2) + 1;
            this.year = c.get(1);
        }

        public boolean equalsTime(long t) {
            return this.millis == t;
        }

        public int hashCode() {
            return Long.valueOf(this.millis).hashCode();
        }

        public boolean equals(Object o) {
            return o != null && (o == this || o instanceof Time && this.equalsTime(((Time) o).millis)
                    || o instanceof Number && this.equalsTime(((Number) o).longValue()));
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.year);
            sb.append(',');
            append00(sb, this.month);
            sb.append(',');
            append00(sb, this.day);
            sb.append(',');
            append00(sb, this.hours);
            sb.append(',');
            append00(sb, this.minutes);
            sb.append(',');
            append00(sb, this.seconds);
            sb.append(',');
            append000(sb, (int) (this.millis % 1000L));
            return sb.toString();
        }

        public int compareTo(Time o) {
            return Long.compare(this.millis, o.millis);
        }

        private static void append00(StringBuilder sb, int i) {
            if (i < 10) {
                sb.append('0');
            }

            sb.append(i);
        }

        private static void append000(StringBuilder sb, int i) {
            if (i < 100) {
                sb.append('0');
            }

            if (i < 10) {
                sb.append('0');
            }

            sb.append(i);
        }

        public String getTime() {
            StringBuilder sb = new StringBuilder();
            append00(sb, this.hours);
            sb.append(':');
            append00(sb, this.minutes);
            sb.append(':');
            append00(sb, this.seconds);
            return sb.toString();
        }

        public String getTimeHMS() {
            StringBuilder sb = new StringBuilder();
            if (this.hours > 0) {
                append00(sb, this.hours);
                sb.append('h');
            }

            if (this.hours > 0 || this.minutes > 0) {
                append00(sb, this.minutes);
                sb.append('m');
            }

            append00(sb, this.seconds);
            sb.append('s');
            return sb.toString();
        }

        public String getDate() {
            StringBuilder sb = new StringBuilder();
            append00(sb, this.day);
            sb.append('.');
            append00(sb, this.month);
            sb.append('.');
            sb.append(this.year);
            return sb.toString();
        }

        public String getDateAndTime() {
            return this.getDate() + ' ' + this.getTime();
        }

        public long getDelta() {
            return Math.abs(now().millis - this.millis);
        }

        public static Time get(Calendar c) {
            return new Time(c);
        }

        public static Time get(long millis) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(millis);
            return get(c);
        }

        public static Time get(int year, int month, int day, int hours, int minutes, int seconds, long deltaMillis) {
            Calendar c = Calendar.getInstance();
            c.set(year, month, day, hours, minutes, seconds);
            c.setTimeInMillis(c.getTimeInMillis() + deltaMillis);
            return null;
        }

        public static Time now() {
            return get(Calendar.getInstance());
        }

        public JsonElement getJson() {
            return new JsonPrimitive(this.millis);
        }

        public static Time deserialize(JsonElement e) {
            return e != null && e.isJsonPrimitive() ? get(e.getAsLong()) : null;
        }
    }

}
