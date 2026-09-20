# Using external backup holds

External holds let a backup tool capture files from a dedicated server without
the usual `save-off` / `save-all` / sleep / `save-on` sequence. Server Utilities
prepares a consistent point and temporarily holds world saving; your tool takes
the snapshot or copies the files. The feature is off by default.

## Enable the commands

Set `B:enable_external_holds=true` in the `backups` section of
`serverutilities/serverutilities.cfg` under the server working directory. The
`commands` section must also have `B:backup=true` (its default). Restart the
server after changing these settings. Only the dedicated server console and
RCON can use `backup hold`; in-game players cannot. At the server console, enter
the whole command on one line without a leading `/`.

The default lease is 600 seconds, the maximum is 1800 seconds, and preparation
and release commands wait up to 120 seconds. Configure these with
`external_hold_default_seconds`, `external_hold_max_seconds`, and
`external_hold_prepare_timeout_seconds` in the same `backups` section.
`external_hold_warn_seconds` defaults to 300 and controls both the first
long-hold warning and its repeat interval. Renewal does not reset total hold
time. An explicit duration must be a positive number of seconds; omit it to
use the default. A default above the maximum is clamped too.

Set the RCON client's command/read timeout above the server's command timeout,
with a network margin (for example, 150 seconds for the default 120). A lost
`begin` reply does not cancel preparation: a hold may still be granted and will
expire automatically. An admin can inspect `status` or run `backup stop` to
release it early. A timed-out `end` may still finish; discard that capture
because its release was not confirmed. Internal `backup start` and `backup stop`
over RCON also use this command wait limit.

## Take a capture

1. Send `backup hold begin [seconds]`. Continue only after a reply such as
   `OK <token> <seconds-left>`. Save the returned token. `begin` saves players
   and worlds and waits for queued file writes before replying. A requested
   duration above the maximum is shortened and adds `CLAMPED` to the reply.
2. Capture the files while the hold is active. For a filesystem or VM snapshot,
   make the immutable snapshot now; you can copy *from that snapshot* after the
   hold ends. For a direct copy such as rsync, keep the hold active until the
   copy finishes. Send `backup hold renew <token> [seconds]` before the reported
   time runs out if needed; use its new `seconds-left` value.
3. Send `backup hold end <token>`. Publish or keep the capture only if the reply
   is `OK`. World saving resumes on release. An expired lease, `backup stop`,
   or server shutdown invalidates the capture even if the snapshot or copy
   appeared to finish.

For a short snapshot, create the immutable snapshot with your own tool between
these two commands, then substitute the returned token in `end`:

| Console or RCON command | Required reply |
| --- | --- |
| `backup hold begin 600` | `OK <token> <seconds-left>` |
| `backup hold end <token>` | `OK` |

`backup hold status` reports an active hold as `OK <token> <seconds-left>` or
returns `NO_HOLD` when idle. It can return `BUSY` during preparation or release.
It is useful for inspection; it does not replace the `OK` reply from `end` as
proof that your capture completed within the hold.
Run one backup script at a time because vanilla RCON shares its reply buffer
between clients.

## Choose what to capture

Capture the complete world save directory selected by `level-name` in
`server.properties`, including its dimension and `serverutilities` data. If
your backup must preserve Server Utilities ranks and player assignments, also
capture `serverutilities/server/ranks.txt` and
`serverutilities/server/players.txt` under the server working directory. These
two files are outside the world save directory. Server Utilities creates no
archive or staging copy for you. If the paths lie on separate filesystems,
snapshot each one before ending the hold.

The hold defers normal player logout and stats saves, and rank-file writes,
until release. Other mods with independent background writers are outside its
barrier. Without Hodgepodge, Minecraft's queued-I/O drain cannot report write
failures or fsync chunk region files. Hodgepodge 2.7.153 has the same failure
reporting limit. If `threadedWorldDataSaving` is active in a newer Hodgepodge
with `WorldDataSaver.flush()`, failed world-data writes make `begin` return
`SAVE_FAILED`; chunk region files still have the vanilla limit.

## Treat uncertain replies as failure

An `OK` release confirms the capture interval, not the durability of subsequent
live saves. Exceptions escaping deferred player or rank saves are logged and
retried every 30 seconds; they do not invalidate an already completed capture.
Pending player saves continue to block reconnect and new holds until the save
call succeeds. Administrators should correct the underlying error shown in the
log; queued player data is not discarded after an arbitrary retry count.
Vanilla player/stat saving catches ordinary disk errors internally, and rank
disk writes are asynchronous: SU cannot detect or retry every such failure.
Check the server log when diagnosing saving problems, even after an `OK` reply.

`BUSY` means another hold, internal backup, preparation, release, or unfinished
drain is in progress; wait and start a new attempt. `DISABLED`, `DENIED`, and
`INVALID_ARGUMENT` mean the request cannot start. `SAVE_FAILED` and `TIMEOUT`
mean preparation or release did not confirm. `EXPIRED` and `BAD_TOKEN` mean
the lease is no longer yours. Discard the capture after any of these replies
or a lost RCON reply.
Never infer success from `status` or from files that happen to exist.

A hold keeps the server ticking even when it would normally pause while empty.
Preparing a hold can stall a tick while dirty chunks save. The watchdog releases
an expired hold and logs a warning; `backup stop` lets an admin release one
early. Restore an external capture only by placing its files on a stopped
server.
