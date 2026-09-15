package serverutils.task.backup;

import javax.annotation.Nullable;

/**
 * A single-line reply to an external backup hold command. Rendered as plain ASCII with a leading result code so a shell
 * script can branch on the first field without parsing.
 */
public final class HoldResponse {

    public final HoldResult result;
    @Nullable
    public final String token;
    public final long secondsLeft;
    public final boolean clamped;

    private HoldResponse(HoldResult result, @Nullable String token, long secondsLeft, boolean clamped) {
        this.result = result;
        this.token = token;
        this.secondsLeft = secondsLeft;
        this.clamped = clamped;
    }

    public static HoldResponse of(HoldResult result) {
        return new HoldResponse(result, null, 0L, false);
    }

    public static HoldResponse granted(String token, long secondsLeft, boolean clamped) {
        return new HoldResponse(HoldResult.OK, token, secondsLeft, clamped);
    }

    public boolean isOk() {
        return result == HoldResult.OK;
    }

    /**
     * {@code OK <token> <seconds-left>}, with {@code CLAMPED} appended when the requested lease was longer than the
     * configured ceiling, so a client learns to shorten its work now rather than discovering it at expiry.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(result.name());
        if (token != null) {
            builder.append(' ').append(token).append(' ').append(secondsLeft);
            if (clamped) builder.append(" CLAMPED");
        }
        return builder.toString();
    }
}
