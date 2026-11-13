package serverutils.lib.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TextVariablesTest {

    @Test
    public void replace_FillsVariablesAndKeepsOtherBraces() {
        Map<String, String> values = new HashMap<>();
        values.put("tps", "20.0");
        values.put("team", "$1 \\ Miners");

        assertEquals(
                "&7TPS 20.0 {unknown} {} {not a variable} [$1 \\ Miners]",
                TextVariables.replace("&7TPS {tps} {unknown} {} {not a variable} [{team}]", values::get));
    }

    @Test
    public void formatDuration_ShowsTheTwoLargestUnits() {
        assertEquals("42s", TextVariables.formatDuration(42_999L));
        assertEquals("7m", TextVariables.formatDuration(7 * 60_000L + 30_000L));
        assertEquals("3h 5m", TextVariables.formatDuration(3 * 3_600_000L + 5 * 60_000L));
        assertEquals("2d 4h", TextVariables.formatDuration(52 * 3_600_000L + 59 * 60_000L));
    }

    @Test
    public void formatWorldTime_StartsTheDayAtSix() {
        assertEquals("06:00", TextVariables.formatWorldTime(0L));
        assertEquals("12:00", TextVariables.formatWorldTime(6000L));
        assertEquals("00:00", TextVariables.formatWorldTime(18000L));
        assertEquals("05:59", TextVariables.formatWorldTime(23999L));
        assertEquals("07:30", TextVariables.formatWorldTime(24000L * 3 + 1500L));
    }
}
