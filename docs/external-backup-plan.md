# External backup holds: remaining validation

Status (2026-09-15): `1714b6c7` planned the feature and `734653e4`
implemented the first version. The follow-up fixes ownership, bounded
preparation, release races, player and rank writes, and Hodgepodge error
reporting. The feature is dedicated-server only and defaults off behind
`enable_external_holds`.

## Behavior

Console and RCON use `backup hold begin [seconds]`, `renew <token> [seconds]`,
`end <token>`, and `status`. SU saves players and worlds, waits for queued
writes, then acknowledges one leased hold. It snapshots, copies, archives,
and runs no OS commands. A watchdog resumes saving when a lease or
preparation expires. A stale release cannot resume a newer hold, and
renewal cannot confirm a hold that is being released. Internal backups
skip a hold; a rejected hold cannot resume an internal backup.

Player and stats saves from the normal player-save path, and rank-file
writes, are deferred until release. A player with a deferred logout save
cannot reconnect and load stale player data before that release. The
covered paths and the response-code rules are documented in the README.

## Limits

Vanilla chunk I/O does not report write failures or fsync region files.
When Hodgepodge's `threadedWorldDataSaving` is active and its version provides
`WorldDataSaver.flush()`, the hold returns `SAVE_FAILED` if world-data writes
still fail. Older versions, including 2.7.153, provide only the queued-I/O
barrier and cannot confirm write failures. Other mods with independent
background writers are outside SU's barrier. `begin` stalls a server tick
while saving dirty chunks.
Vanilla RCON shares one output buffer, so two backup scripts must not run
concurrently.

## Before release

Validate the built mod on a dedicated server over real RCON, with and
without Hodgepodge. Exercise an empty paused server, active players,
logout and reconnect, rank edits, dimension load and unload, a killed
script, `backup stop`, vanilla save commands, and normal and crash
shutdown. Record the Hodgepodge version and whether
`threadedWorldDataSaving` was enabled. Unit tests cannot prove the RCON
reply path or real mixin interaction.

Native filesystem snapshot integration, remote file transport, upload,
retention, and server-side restore are outside this branch.
