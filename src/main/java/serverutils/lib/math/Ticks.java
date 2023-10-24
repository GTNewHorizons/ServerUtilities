package serverutils.lib.math;

import serverutils.lib.util.StringUtils;

public class Ticks {

    public static final Ticks NO_TICKS = new Ticks(0L);
    public static final Ticks ONE_TICK = new Ticks(1L);
    public static final Ticks SECOND = ONE_TICK.x(20L);
    public static final long TICK_MS = 1000L / SECOND.ticks();
    public static final Ticks MINUTE = SECOND.x(60L);
    public static final Ticks HOUR = MINUTE.x(60L);
    public static final Ticks DAY = HOUR.x(24L);
    public static final Ticks WEEK = DAY.x(7L);

    public static Ticks get(long ticks) {
        if (ticks == 0L) {
            return NO_TICKS;
        } else if (ticks == 1L) {
            return ONE_TICK;
        }

        return new Ticks(Math.max(0L, ticks));
    }

    public static Ticks getFromMillis(long millis) {
        return get(millis / TICK_MS);
    }

    public static Ticks get(String value) throws NumberFormatException {
        if (value.isEmpty() || value.equals("0s")) {
            return NO_TICKS;
        } else if (value.length() == 2 && value.charAt(0) == '1') {
            switch (value.charAt(1)) {
                case 't':
                case 'T':
                    return ONE_TICK;
                case 's':
                case 'S':
                    return SECOND;
                case 'm':
                case 'M':
                    return MINUTE;
                case 'h':
                case 'H':
                    return HOUR;
                case 'd':
                case 'D':
                    return DAY;
                case 'w':
                case 'W':
                    return WEEK;
            }
        }

        Ticks ticks = Ticks.NO_TICKS;

        for (String s : value.split(" ")) {
            if (!s.isEmpty()) {
                try {
                    ticks = switch (s.charAt(s.length() - 1)) {
                        case 't', 'T' -> ticks.add(Long.parseLong(s.substring(0, s.length() - 1)));
                        case 's', 'S' -> ticks.add(SECOND.x(Long.parseLong(s.substring(0, s.length() - 1))));
                        case 'm', 'M' -> ticks.add(MINUTE.x(Long.parseLong(s.substring(0, s.length() - 1))));
                        case 'h', 'H' -> ticks.add(HOUR.x(Long.parseLong(s.substring(0, s.length() - 1))));
                        case 'd', 'D' -> ticks.add(DAY.x(Long.parseLong(s.substring(0, s.length() - 1))));
                        case 'w', 'W' -> ticks.add(WEEK.x(Long.parseLong(s.substring(0, s.length() - 1))));
                        default -> ticks.add(Long.parseLong(s));
                    };
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        return ticks;
    }

    private final long ticks;

    private Ticks(long t) {
        ticks = t;
    }

    public long ticks() {
        return ticks;
    }

    public boolean hasTicks() {
        return ticks > 0L;
    }

    public Ticks x(long x) {
        return x == 1L ? this : get(ticks * x);
    }

    public Ticks x(double x) {
        return x == 1D ? this : get((long) (ticks * x));
    }

    public Ticks add(long t) {
        return t == 0L ? this : get(ticks + t);
    }

    public Ticks add(Ticks t) {
        return add(t.ticks);
    }

    public long millis() {
        return ticks * TICK_MS;
    }

    public long seconds() {
        return ticks / SECOND.ticks;
    }

    public double secondsd() {
        return (double) ticks / (double) SECOND.ticks;
    }

    public long minutes() {
        return ticks / MINUTE.ticks;
    }

    public double minutesd() {
        return (double) ticks / (double) MINUTE.ticks;
    }

    public long hours() {
        return ticks / HOUR.ticks;
    }

    public double hoursd() {
        return (double) ticks / (double) HOUR.ticks;
    }

    public long days() {
        return ticks / DAY.ticks;
    }

    public double daysd() {
        return (double) ticks / (double) DAY.ticks;
    }

    public long weeks() {
        return ticks / WEEK.ticks;
    }

    public double weeksd() {
        return (double) ticks / (double) WEEK.ticks;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof Ticks && equalsTimer((Ticks) o);
    }

    public boolean equalsTimer(Ticks t) {
        return ticks == t.ticks;
    }

    public int hashCode() {
        return Long.hashCode(ticks);
    }

    public String toString() {
        if (ticks <= 0L) {
            return "0s";
        }

        StringBuilder builder = new StringBuilder();

        if (ticks < 20L) {
            builder.append(ticks);
            builder.append('t');
            return builder.toString();
        }

        long weeks = weeks();
        boolean hasWeeks = weeks > 0L;

        if (hasWeeks) {
            builder.append(weeks);
            builder.append('w');
        }

        long days = days() % 7L;
        boolean hasDays = hasWeeks || days != 0L;

        if (days != 0L) {
            if (hasWeeks) {
                builder.append(' ');
            }

            builder.append(days);
            builder.append('d');
        }

        long hours = hours() % 24L;
        boolean hasHours = hasDays || hours != 0L;

        if (hours != 0L) {
            if (hasDays) {
                builder.append(' ');
            }

            builder.append(hours);
            builder.append('h');
        }

        long minutes = minutes() % 60L;
        boolean hasMinutes = hasHours || minutes != 0L;

        if (minutes != 0L) {
            if (hasHours) {
                builder.append(' ');
            }

            builder.append(minutes);
            builder.append('m');
        }

        long seconds = seconds() % 60L;
        boolean hasSeconds = hasMinutes || seconds != 0L;

        if (seconds != 0L) {
            if (hasMinutes) {
                builder.append(' ');
            }

            builder.append(seconds);
            builder.append('s');
        }

        long t = ticks % 20L;

        if (t != 0L) {
            if (hasSeconds) {
                builder.append(' ');
            }

            builder.append(t);
            builder.append('t');
        }

        return builder.toString();
    }

    public String toTimeString() {
        return StringUtils.getTimeString(millis());
    }
}
