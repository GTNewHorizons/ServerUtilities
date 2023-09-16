package latmod.lib;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class LMStringUtils {

    public static final int DAY24 = 24 * 60 * 60;
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public static final String STRIP_SEP = ", ";
    public static final String ALLOWED_TEXT_CHARS = " -_!@#$%^&*()+=\\/,.<>?\'\"[]{}|;:`~";

    public static final Comparator<Object> ignoreCaseComparator = new Comparator<Object>() {

        public int compare(Object o1, Object o2) {
            return String.valueOf(o1).compareToIgnoreCase(String.valueOf(o2));
        }
    };

    public static boolean isValid(String s) {
        return s != null && s.length() > 0;
    }

    public static String[] shiftArray(String[] s) {
        if (s == null || s.length == 0) return new String[0];
        String[] s1 = new String[s.length - 1];
        System.arraycopy(s, 1, s1, 0, s1.length);
        return s1;
    }

    public static String readString(InputStream is) throws Exception {
        final char[] buffer = new char[0x10000];
        final StringBuilder out = new StringBuilder();
        try (Reader in = new InputStreamReader(is, "UTF-8")) {
            int read;
            do {
                read = in.read(buffer, 0, buffer.length);
                if (read > 0) {
                    out.append(buffer, 0, read);
                }
            } while (read >= 0);
        }
        return out.toString();
    }

    public static List<String> readStringList(InputStream is) throws Exception {
        ArrayList<String> l = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String s;
        while ((s = reader.readLine()) != null) l.add(s);
        reader.close();
        return l;
    }

    public static List<String> toStringList(String s, String regex) {
        ArrayList<String> al = new ArrayList<>();
        String[] s1 = s.split(regex);
        if (s1.length > 0) for (String aS1 : s1) al.add(aS1.trim());
        return al;
    }

    public static String fromStringList(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            sb.append(l.get(i));
            if (i != l.size() - 1) sb.append('\n');
        }
        return sb.toString();
    }

    public static boolean isASCIIChar(char c) {
        return c > 0 && c < 256;
    }

    public static boolean isTextChar(char c, boolean onlyAZ09) {
        if (!isASCIIChar(c)) return false;
        if (c >= '0' && c <= '9') return true;
        if (c >= 'a' && c <= 'z') return true;
        return c >= 'A' && c <= 'Z' || !onlyAZ09 && (ALLOWED_TEXT_CHARS.indexOf(c) != -1);
    }

    public static void replace(List<String> txt, String s, String s1) {
        if (txt == null || txt.isEmpty() || s == null || s1 == null) return;
        String s2;
        for (int i = 0; i < txt.size(); i++) {
            s2 = txt.get(i);
            if (s2 != null && s2.length() > 0) {
                s2 = s2.replace(s, s1);
                txt.set(i, s2);
            }
        }
    }

    public static String replace(String s, char c, char with) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c1 = s.charAt(i);
            sb.append((c1 == c) ? with : c1);
        }
        return sb.toString();
    }

    public static <E> String[] toStrings(E[] o) {
        if (o == null) return null;
        String[] s = new String[o.length];
        for (int i = 0; i < o.length; i++) s[i] = String.valueOf(o[i]);
        return s;
    }

    public static String strip(String... o) {
        if (o == null) return null;
        if (o.length == 0) return "";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < o.length; i++) {
            sb.append(o[i]);
            if (i != o.length - 1) sb.append(STRIP_SEP);
        }

        return sb.toString();
    }

    public static String strip(Collection<?> c) {
        if (c == null) return null;
        if (c.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();

        int idx = 0;
        int eidx = c.size() - 1;
        for (Object o : c) {
            sb.append(o);
            if (idx != eidx) sb.append(STRIP_SEP);
            idx++;
        }

        return sb.toString();
    }

    public static String stripD(double... o) {
        if (o == null) return null;
        if (o.length == 0) return "";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < o.length; i++) {
            sb.append(MathHelperLM.toSmallDouble(o[i]));
            if (i != o.length - 1) sb.append(STRIP_SEP);
        }

        return sb.toString();
    }

    public static String stripDI(double... o) {
        if (o == null) return null;
        if (o.length == 0) return "";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < o.length; i++) {
            sb.append((long) o[i]);
            if (i != o.length - 1) sb.append(STRIP_SEP);
        }

        return sb.toString();
    }

    public static String stripI(int... o) {
        if (o == null) return null;
        if (o.length == 0) return "";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < o.length; i++) {
            sb.append(o[i]);
            if (i != o.length - 1) sb.append(STRIP_SEP);
        }

        return sb.toString();
    }

    public static String stripB(boolean... o) {
        if (o == null) return null;
        if (o.length == 0) return "";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < o.length; i++) {
            sb.append(o[i] ? '1' : '0');
            if (i != o.length - 1) sb.append(STRIP_SEP);
        }

        return sb.toString();
    }

    public static String stripB(byte... o) {
        if (o == null) return null;
        if (o.length == 0) return "";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < o.length; i++) {
            sb.append(o[i]);
            if (i != o.length - 1) sb.append(STRIP_SEP);
        }

        return sb.toString();
    }

    public static String unsplit(String[] s, String s1) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        if (s.length == 1) return s[0];
        for (int i = 0; i < s.length; i++) {
            sb.append(s[i]);
            if (i != s.length - 1) sb.append(s1);
        }
        return sb.toString();
    }

    public static String unsplit(Object[] o, String s1) {
        if (o == null) return null;
        if (o.length == 1) return String.valueOf(o[0]);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < o.length; i++) {
            sb.append(o[i]);
            if (i != o.length - 1) sb.append(s1);
        }
        return sb.toString();
    }

    public static String unsplitSpaceUntilEnd(int startIndex, String[] o) {
        if (o == null || startIndex < 0 || o.length <= startIndex) return null;
        StringBuilder sb = new StringBuilder();

        for (int i = startIndex; i < o.length; i++) {
            sb.append(o[i]);
            if (i != o.length - 1) sb.append(' ');
        }

        return sb.toString();
    }

    public static String firstUppercase(String s) {
        if (s == null || s.length() == 0) return s;
        char c = Character.toUpperCase(s.charAt(0));
        if (s.length() == 1) return Character.toString(c);
        return String.valueOf(c) + s.substring(1);
    }

    public static boolean areStringsEqual(String s0, String s1) {
        if (s0 == null && s1 == null) return true;
        if (s0 == null || s1 == null) return false;
        if (s0.length() != s1.length()) return false;
        return s0.isEmpty() && s1.isEmpty() || s0.equals(s1);
    }

    public static String fillString(String s, char fill, int length) {
        int sl = s.length();

        char[] c = new char[Math.max(sl, length)];

        for (int i = 0; i < c.length; i++) {
            if (i >= sl) c[i] = fill;
            else c[i] = s.charAt(i);
        }

        return new String(c);
    }

    public static boolean contains(String[] s, String s1) {
        if (s == null || s1 == null || s.length == 0) return false;
        for (String value : s) if (value != null && (value == s1 || value.equals(s1))) return true;
        return false;
    }

    public static String substring(String s, String pre, String post) {
        int preI = s.indexOf(pre);
        int postI = s.lastIndexOf(post);
        return s.substring(preI + 1, postI);
    }

    public static String substring(String s, char pre, char post) {
        int preI = s.indexOf(pre);
        int postI = s.lastIndexOf(post);
        return s.substring(preI + 1, postI);
    }

    public static String removeAllWhitespace(String s) {
        if (s == null || s.isEmpty()) return s;
        else return s.replaceAll("\\s+", "");
    }

    public static String trimAllWhitespace(String s) {
        if (s == null || s.isEmpty()) return s;
        else return s.replace("^\\s*(.*?)\\s*$", "$1");
    }

    public static String formatInt(int i) {
        return formatInt(i, 1);
    }

    public static String formatInt(int i, int z) {
        String s0 = Integer.toString(i);
        if (z <= 0) return s0;
        z += 1;

        StringBuilder sb = new StringBuilder();

        int l = z - s0.length();
        for (int j = 0; j < l; j++) sb.append('0');

        sb.append(i);
        return sb.toString();
    }

    public static String formatDouble(double d) {
        if (d == Double.POSITIVE_INFINITY) return "+Inf";
        else if (d == Double.NEGATIVE_INFINITY) return "-Inf";
        else if (d == Double.NaN) return "NaN";

        d = ((long) (d * 1000D)) / 1000D;
        String s = String.valueOf(d);
        if (s.endsWith(".0")) return s.substring(0, s.length() - 2);
        return s;
    }

    public static String getTimeString(long millis) {
        return getTimeString(millis, true);
    }

    public static String getTimeString(long millis, boolean days) {
        long secs = millis / 1000L;
        StringBuilder sb = new StringBuilder();

        long h = (secs / 3600L) % 24;
        long m = (secs / 60L) % 60L;
        long s = secs % 60L;

        if (days && secs >= DAY24) {
            sb.append(secs / DAY24);
            // sb.append("d ");
            sb.append(':');
        }

        if (h < 10) sb.append('0');
        sb.append(h);
        // sb.append("h ");
        sb.append(':');
        if (m < 10) sb.append('0');
        sb.append(m);
        // sb.append("m ");
        sb.append(':');
        if (s < 10) sb.append('0');
        sb.append(s);
        // sb.append('s');

        return sb.toString();
    }

    public static byte[] toBytes(String s) {
        if (s == null) return null;
        else if (s.length() == 0) return new byte[0];
        else {
            byte[] b = new byte[s.length()];
            for (int i = 0; i < b.length; i++) b[i] = (byte) s.charAt(i);
            return b;
        }
    }

    public static String fromBytes(byte[] b) {
        if (b == null) return null;
        else if (b.length == 0) return "";
        else {
            char[] c = new char[b.length];
            for (int i = 0; i < b.length; i++) c[i] = (char) (b[i] & 0xFF);
            return new String(c);
        }
    }
}
