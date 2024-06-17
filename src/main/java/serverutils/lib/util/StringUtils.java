package serverutils.lib.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import serverutils.lib.io.Bits;

public class StringUtils {

    public static final String ALLOWED_TEXT_CHARS = " .-_!@#$%^&*()+=\\/,<>?\'\"[]{}|;:`~";
    public static final char FORMATTING_CHAR = '\u00a7';
    public static final String[] EMPTY_ARRAY = {};
    public static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static final int FLAG_ID_ALLOW_EMPTY = 1;
    public static final int FLAG_ID_FIX = 2;
    public static final int FLAG_ID_ONLY_LOWERCASE = 4;
    public static final int FLAG_ID_ONLY_UNDERLINE = 8;
    public static final int FLAG_ID_ONLY_UNDERLINE_OR_PERIOD = FLAG_ID_ONLY_UNDERLINE | 16;
    public static final int FLAG_ID_DEFAULTS = FLAG_ID_FIX | FLAG_ID_ONLY_LOWERCASE | FLAG_ID_ONLY_UNDERLINE;
    public static final int FLAG_ID_PERIOD_DEFAULTS = FLAG_ID_FIX | FLAG_ID_ONLY_LOWERCASE
            | FLAG_ID_ONLY_UNDERLINE_OR_PERIOD;

    public static final Comparator<Object> IGNORE_CASE_COMPARATOR = (o1, o2) -> String.valueOf(o1)
            .compareToIgnoreCase(String.valueOf(o2));
    public static final Comparator<Object> ID_COMPARATOR = (o1, o2) -> getID(o1, FLAG_ID_FIX)
            .compareToIgnoreCase(getID(o2, FLAG_ID_FIX));

    public static final Map<String, String> TEMP_MAP = new HashMap<>();
    public static final DecimalFormat DOUBLE_FORMATTER_00 = new DecimalFormat("#0.00");
    public static final DecimalFormat DOUBLE_FORMATTER_0 = new DecimalFormat("#0.0");
    public final static int[] INT_SIZE_TABLE = { 9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999,
            Integer.MAX_VALUE };

    private static final Pattern NOT_SNAKE_CASE_PATTERN = Pattern.compile("[^a-z0-9_]");
    private static final Pattern REPEATING_UNDERSCORE_PATTERN = Pattern.compile("_{2,}");
    private static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)[\\&\u00a7]([0-9A-FK-OR])");

    static {
        DOUBLE_FORMATTER_00.setRoundingMode(RoundingMode.DOWN);
        DOUBLE_FORMATTER_0.setRoundingMode(RoundingMode.DOWN);
    }

    public static String unformatted(String string) {
        return string.isEmpty() ? string : FORMATTING_CODE_PATTERN.matcher(string).replaceAll("");
    }

    public static String addFormatting(String string) {
        return FORMATTING_CODE_PATTERN.matcher(string).replaceAll("\u00a7$1");
    }

    public static String toSnakeCase(String string) {
        return string.isEmpty() ? string
                : REPEATING_UNDERSCORE_PATTERN
                        .matcher(NOT_SNAKE_CASE_PATTERN.matcher(unformatted(string).toLowerCase()).replaceAll("_"))
                        .replaceAll("_");
    }

    public static String emptyIfNull(@Nullable Object o) {
        return o == null ? "" : o.toString();
    }

    public static String getRawID(Object o) {
        if (o instanceof IStringSerializable serializable) {
            return serializable.getName();
        } else if (o instanceof IWithID id) {
            return id.getId();
        } else if (o instanceof Enum) {
            return ((Enum) o).name();
        }

        return String.valueOf(o);
    }

    public static String getID(Object o, int flags) {
        String id = getRawID(o);

        if (flags == 0) {
            return id;
        }

        boolean fix = Bits.getFlag(flags, FLAG_ID_FIX);

        if (!fix && id.isEmpty() && !Bits.getFlag(flags, FLAG_ID_ALLOW_EMPTY)) {
            throw new NullPointerException("ID can't be empty!");
        }

        if (Bits.getFlag(flags, FLAG_ID_ONLY_LOWERCASE)) {
            if (fix) {
                id = id.toLowerCase();
            } else if (!id.equals(id.toLowerCase())) {
                throw new IllegalArgumentException("ID can't contain uppercase characters!");
            }
        }

        if (Bits.getFlag(flags, FLAG_ID_ONLY_UNDERLINE)) {
            if (fix) {
                id = id.toLowerCase();
            } else if (!id.equals(id.toLowerCase())) {
                throw new IllegalArgumentException("ID can't contain uppercase characters!");
            }
        }

        if (Bits.getFlag(flags, FLAG_ID_ONLY_UNDERLINE)) {
            boolean allowPeriod = Bits.getFlag(flags, 16);

            char[] chars = id.toCharArray();

            for (int i = 0; i < chars.length; i++) {
                if (!(chars[i] == '.' && allowPeriod || isTextChar(chars[i], true))) {
                    if (fix) {
                        chars[i] = '_';
                    } else {
                        throw new IllegalArgumentException("ID contains invalid character: '" + chars[i] + "'!");
                    }
                }
            }

            id = new String(chars);
        }

        return id;
    }

    public static String[] shiftArray(@Nullable String[] s) {
        if (s == null || s.length <= 1) {
            return EMPTY_ARRAY;
        }

        String[] s1 = new String[s.length - 1];
        System.arraycopy(s, 1, s1, 0, s1.length);
        return s1;
    }

    public static boolean isASCIIChar(char c) {
        return c > 0 && c < 256;
    }

    public static boolean isTextChar(char c, boolean onlyAZ09) {
        return isASCIIChar(c) && (c >= '0' && c <= '9' || c >= 'a' && c <= 'z'
                || c >= 'A' && c <= 'Z'
                || !onlyAZ09 && (ALLOWED_TEXT_CHARS.indexOf(c) != -1));
    }

    public static void replace(List<String> txt, String s, String s1) {
        if (!txt.isEmpty()) {
            String s2;
            for (int i = 0; i < txt.size(); i++) {
                s2 = txt.get(i);
                if (s2 != null && !s2.isEmpty()) {
                    s2 = s2.replace(s, s1);
                    txt.set(i, s2);
                }
            }
        }
    }

    public static String replace(String s, char c, char with) {
        if (s.isEmpty()) {
            return s;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c1 = s.charAt(i);
            sb.append((c1 == c) ? with : c1);
        }

        return sb.toString();
    }

    public static String joinSpaceUntilEnd(int startIndex, CharSequence[] o) {
        if (startIndex < 0 || o.length <= startIndex) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = startIndex; i < o.length; i++) {
            sb.append(o[i]);
            if (i != o.length - 1) {
                sb.append(' ');
            }
        }

        return sb.toString();
    }

    public static String firstUppercase(String s) {
        if (s.isEmpty()) {
            return s;
        }
        char c = Character.toUpperCase(s.charAt(0));
        if (s.length() == 1) {
            return Character.toString(c);
        }
        return c + s.substring(1);
    }

    public static String fillString(CharSequence s, char fill, int length) {
        int sl = s.length();

        char[] c = new char[Math.max(sl, length)];

        for (int i = 0; i < c.length; i++) {
            if (i >= sl) {
                c[i] = fill;
            } else {
                c[i] = s.charAt(i);
            }
        }

        return new String(c);
    }

    public static String removeAllWhitespace(String s) {
        char[] chars = new char[s.length()];
        int j = 0;

        for (int i = 0; i < chars.length; i++) {
            char c = s.charAt(i);

            if (c > ' ') {
                chars[j] = c;
                j++;
            }
        }

        return new String(chars, 0, j);
    }

    public static String readString(InputStream is) throws Exception {
        char[] buffer = new char[65536];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(is, "UTF-8");
        Throwable var4 = null;

        try {
            int read;
            try {
                do {
                    read = in.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        out.append(buffer, 0, read);
                    }
                } while (read >= 0);
            } catch (Throwable var13) {
                var4 = var13;
                throw var13;
            }
        } finally {
            if (in != null) {
                if (var4 != null) {
                    try {
                        in.close();
                    } catch (Throwable var12) {
                        var4.addSuppressed(var12);
                    }
                } else {
                    in.close();
                }
            }

        }

        return out.toString();
    }

    public static List<String> readStringList(InputStream is) throws Exception {
        ArrayList<String> l = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        String s;
        while ((s = reader.readLine()) != null) {
            l.add(s);
        }
        reader.close();
        return l;
    }

    public static String fromStringList(List<String> l) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < l.size(); ++i) {
            sb.append(l.get(i));
            if (i != l.size() - 1) {
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    public static String formatDouble0(double value) {
        String s = DOUBLE_FORMATTER_0.format(value);
        return s.endsWith(".00") ? s.substring(0, s.length() - 2) : s;
    }

    public static String formatDouble00(double value) {
        String s = DOUBLE_FORMATTER_00.format(value);
        return s.endsWith(".00") ? s.substring(0, s.length() - 3) : s;
    }

    public static String formatDouble(double value, boolean fancy) {
        if (Double.isNaN(value)) {
            return "NaN";
        } else if (value == Double.POSITIVE_INFINITY) {
            return "+Inf";
        } else if (value == Double.NEGATIVE_INFINITY) {
            return "-Inf";
        } else if (value == Long.MAX_VALUE) {
            return "2^63-1";
        } else if (value == Long.MIN_VALUE) {
            return "-2^63";
        } else if (value == 0D) {
            return "0";
        } else if (!fancy) {
            return formatDouble00(value);
        } else if (value >= 1000000000D) {
            return formatDouble00(value / 1000000000D) + "B";
        } else if (value >= 1000000D) {
            return formatDouble00(value / 1000000D) + "M";
        } else if (value >= 10000D) {
            return formatDouble00(value / 1000D) + "K";
        }

        return formatDouble00(value);
    }

    public static String formatDouble(double value) {
        return formatDouble(value, false);
    }

    public static String getTimeString(long millis) {
        boolean neg = false;
        if (millis < 0L) {
            neg = true;
            millis = -millis;
        }

        StringBuilder sb = new StringBuilder();

        if (millis < 1000L) {
            if (neg) {
                sb.append('-');
            }

            sb.append(millis);
            sb.append('m');
            sb.append('s');
            return sb.toString();
        }

        long secs = millis / 1000L;

        if (neg) {
            sb.append('-');
        }

        long h = (secs / 3600L) % 24;
        long m = (secs / 60L) % 60L;
        long s = secs % 60L;

        if (secs >= 86400L) {
            sb.append(secs / 86400L);
            sb.append('d');
            sb.append(' ');
        }

        if (h > 0 || secs >= 86400L) {
            if (h < 10) {
                sb.append('0');
            }
            sb.append(h);
            // sb.append("h ");
            sb.append(':');
        }

        if (m < 10) {
            sb.append('0');
        }
        sb.append(m);
        // sb.append("m ");
        sb.append(':');
        if (s < 10) {
            sb.append('0');
        }
        sb.append(s);
        // sb.append('s');

        return sb.toString();
    }

    public static String fromUUID(@Nullable UUID id) {
        if (id != null) {
            long msb = id.getMostSignificantBits();
            long lsb = id.getLeastSignificantBits();
            StringBuilder sb = new StringBuilder(32);
            digitsUUID(sb, msb >> 32, 8);
            digitsUUID(sb, msb >> 16, 4);
            digitsUUID(sb, msb, 4);
            digitsUUID(sb, lsb >> 48, 4);
            digitsUUID(sb, lsb, 12);
            return sb.toString();
        }

        return "";
    }

    private static void digitsUUID(StringBuilder sb, long val, int digits) {
        long hi = 1L << (digits * 4);
        String s = Long.toHexString(hi | (val & (hi - 1)));
        sb.append(s, 1, s.length());
    }

    @Nullable
    public static UUID fromString(@Nullable String s) {
        if (s == null || !(s.length() == 32 || s.length() == 36)) {
            return null;
        }

        try {
            if (s.indexOf('-') != -1) {
                return UUID.fromString(s);
            }

            int l = s.length();
            StringBuilder sb = new StringBuilder(36);
            for (int i = 0; i < l; i++) {
                sb.append(s.charAt(i));
                if (i == 7 || i == 11 || i == 15 || i == 19) {
                    sb.append('-');
                }
            }

            return UUID.fromString(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, String> parse(Map<String, String> map, String s) {
        if (map == TEMP_MAP) {
            map.clear();
        }

        for (String entry : s.split(",")) {
            String[] val = entry.split("=");

            for (String key : val[0].split("&")) {
                map.put(key, val[1]);
            }
        }

        return map;
    }

    public static IChatComponent color(IChatComponent component, @Nullable EnumChatFormatting color) {
        component.getChatStyle().setColor(color);
        return component;
    }

    public static IChatComponent bold(IChatComponent component, boolean value) {
        component.getChatStyle().setBold(value);
        return component;
    }

    public static IChatComponent italic(IChatComponent component, boolean value) {
        component.getChatStyle().setItalic(value);
        return component;
    }

    public static IChatComponent underlined(IChatComponent component, boolean value) {
        component.getChatStyle().setUnderlined(value);
        return component;
    }

    public static String fixTabs(String string, int tabSize) // FIXME
    {
        String with;

        if (tabSize == 2) {
            with = "  ";
        } else if (tabSize == 4) {
            with = "    ";
        } else {
            char[] c = new char[tabSize];
            Arrays.fill(c, ' ');
            with = new String(c);
        }

        return string.replace("\t", with);
    }

    public static int stringSize(int x) {
        for (int i = 0;; i++) {
            if (x <= INT_SIZE_TABLE[i]) {
                return i + 1;
            }
        }
    }

    public static String add0s(int number, int max) {
        int size = stringSize(max);
        int nsize = stringSize(number);
        StringBuilder builder = new StringBuilder(size);

        for (int i = 0; i < size - nsize; i++) {
            builder.append('0');
        }

        builder.append(number);
        return builder.toString();
    }

    public static String camelCaseToWords(String key) {
        StringBuilder builder = new StringBuilder();
        boolean pu = false;

        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            boolean u = Character.isUpperCase(c);

            if (!pu && u) {
                builder.append(' ');
            }

            pu = u;

            if (i == 0) {
                c = Character.toUpperCase(c);
            }

            builder.append(c);
        }

        return builder.toString();
    }
}
