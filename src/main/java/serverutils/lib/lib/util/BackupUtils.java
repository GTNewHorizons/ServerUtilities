package serverutils.lib.lib.util;

import java.text.DecimalFormat;
import java.util.Calendar;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BackupUtils {

    public static final DecimalFormat smallDoubleFormatter = new DecimalFormat("#0.00");

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
