package serverutils.task.backup;

/**
 * Wire codes for the external backup hold commands. These are protocol, not user-facing text: they are never localised
 * or coloured, and their spellings must stay stable.
 */
public enum HoldResult {

    /** The request succeeded. */
    OK,
    /** Another hold or a backup owns world saving. */
    BUSY,
    /** The sender is not the console or RCON. */
    DENIED,
    /** An argument was missing or not a number. Distinct from DENIED so a typo is not read as a permission failure. */
    INVALID_ARGUMENT,
    /** External holds are turned off in the config. */
    DISABLED,
    /**
     * The hold is gone: the lease ran out, an admin released it, or the server is shutting down. A client seeing this
     * must discard whatever it captured. The causes are deliberately not distinguished, because the correct response to
     * all of them is the same and separating them invites clients to treat one as recoverable.
     */
    EXPIRED,
    /** No hold is active. Only meaningful for status against an idle server. */
    NO_HOLD,
    /** The token does not match the active hold, which means the caller's hold ended. Discard, as for EXPIRED. */
    BAD_TOKEN,
    /** The world could not be saved or flushed, so no consistent point exists. */
    SAVE_FAILED,
    /** The server thread did not answer in time. Nothing can be assumed about world state. */
    TIMEOUT
}
