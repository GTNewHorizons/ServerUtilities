package serverutils.task.backup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BackupDuration {

    private static final Pattern DURATION = Pattern.compile("([1-9][0-9]*)([smhdw])");

    private BackupDuration() {}

    public static long parse(String value) {
        Matcher match = DURATION.matcher(value);
        if (match.matches()) {
            long unit = switch (match.group(2)) {
                case "s" -> 1_000L;
                case "m" -> 60_000L;
                case "h" -> 3_600_000L;
                case "d" -> 86_400_000L;
                default -> 604_800_000L;
            };
            try {
                return Math.multiplyExact(Long.parseLong(match.group(1)), unit);
            } catch (ArithmeticException | NumberFormatException ignored) {}
        }
        throw new IllegalArgumentException("Invalid backup duration: " + value);
    }

    public static String normalizeTimer(String value) {
        if (value == null) throw new IllegalArgumentException("Missing backup_timer");
        String timer = value.trim();
        if (DURATION.matcher(timer).matches()) {
            parse(timer);
            return timer;
        }
        try {
            BigDecimal hours = new BigDecimal(timer);
            if (hours.signum() < 0) throw new NumberFormatException();
            long seconds = Math.max(
                    1,
                    hours.multiply(BigDecimal.valueOf(3600)).setScale(0, RoundingMode.CEILING).longValueExact());
            Math.multiplyExact(seconds, 1000L);
            long[] units = { 604800, 86400, 3600, 60, 1 };
            String[] suffixes = { "w", "d", "h", "m", "s" };
            for (int i = 0; i < units.length; i++) {
                if (seconds % units[i] == 0) return seconds / units[i] + suffixes[i];
            }
        } catch (ArithmeticException | NumberFormatException ignored) {}
        throw new IllegalArgumentException("Invalid backup_timer: " + value + "; use a positive duration such as 30m");
    }
}
