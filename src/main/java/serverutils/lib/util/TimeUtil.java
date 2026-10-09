package serverutils.lib.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TimeUtil {

    public static final DateTimeFormatter NEW_DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    public static String format(ZonedDateTime time) {
        return time.format(NEW_DATE_FORMAT);
    }

    public static String timeAgo(Instant timestamp) {
        Duration duration = Duration.between(timestamp, Instant.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return "just now";
        } else if (seconds < 3600) {
            return duration.toMinutes() + " mins ago";
        } else if (seconds < 86400) {
            return duration.toHours() + " hrs ago";
        } else if (seconds < 604800) {
            return duration.toDays() + " days ago";
        } else if (seconds < 2592000) {
            return duration.toDays() / 7 + " weeks ago";
        } else if (seconds < 31556952) {
            return duration.toDays() / 30 + " mths ago";
        } else {
            return duration.toDays() / 365 + " yrs ago";
        }
    }
}
