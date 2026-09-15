# External backup holds: direction and implementation plan

Status: direction and scope agreed 2026-09-15. Not yet implemented; no code in
this branch depends on it. Targets dedicated servers only. Written against
`algent/ext-backup-api` at `0a97f502`, on top of the save/restore PR. Preserve
that PR's fixes; do not revert to the earlier backup implementation.

The Hodgepodge behaviour described here is from `algent/fix-save-corruption`,
which was still unmerged when this was written.

## Problem

Many server admins do not use SU's own backup. They use filesystem snapshots,
VM-level backups, or their own scripts, for good reasons. The process they have
today is the `save-off` dance described in
https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/issues/25520:

```
rcon: save-off
rcon: save-all
sleep 10
<snapshot / rsync / vm backup>
rcon: save-on
```

It is unreliable in four specific ways:

1. `save-all` returns before the writes land. Chunk writes are queued onto
   `ThreadedFileIOBase` and drained by a separate low-priority thread. The
   `sleep` is a guess, and under load on a large modpack it is sometimes wrong.
2. If the script dies between `save-off` and `save-on`, saving stays off
   silently until someone restarts the server and loses everything since.
3. `save-off` does not cover everything. Player data written on logout,
   dimensions loaded during the window, and SU's own `ranks.txt` / `players.txt`
   are all outside the vanilla toggle.
4. There is no success signal. "Saved", "failed to save", and "still saving"
   are indistinguishable from the outside.

The parent PR already fixes 1 and 3 for SU's internal backup. This work exposes
that same prepared state to an external script, and adds a dead-man's switch
for 2.

## What SU provides

One thing, stated as one sentence:

> Between the acknowledgement of `begin` and the release of the hold, everything
> SU controls has been written to disk, and nothing SU controls will write to
> the backup paths.

SU does not copy, archive, compress, snapshot, elevate, upload, or retain
anything for the external tool, and runs no OS commands on its behalf.

Honest limits, to be stated in the user documentation. How strong the guarantee
is depends on whether Hodgepodge is present, so state both cases:

- Without Hodgepodge, `waitForFinish()` proves writes were handed to the OS, not
  that they are durable, and `writeNextIO()` does not report errors upward. We
  promise consistent, not fsynced, and a write that failed looks like one that
  succeeded.
- With Hodgepodge's `threadedWorldDataSaving`, world data is written to a
  temporary file, `fsync`ed via `FileChannel.force(true)`, and atomically
  renamed, and `WorldDataSaver.flush()` throws listing any file that could not
  be written. For world data the guarantee is genuinely durable and failures
  are detectable. Chunk region files still go through vanilla `RegionFile` and
  are not fsynced.
- Cooperating server-side writers are covered. Arbitrary mods with their own
  background threads are not, and cannot be.
- SU cannot repair corrupt bytes or drain a broken queue through a command.

## Decisions taken

| # | Decision | Reason |
| --- | --- | --- |
| 1 | Hold only. SU stages nothing and copies nothing. | Snapshot tools need a two-second hold; copy tools need the hold to last their copy. Neither needs SU to duplicate the world. Removes all staging, manifest, and temp-disk concerns. |
| 2 | Warn and clamp on long holds. | A 40-minute rsync against a live server should be a deliberate choice, visible in the log, not an accident. |
| 3 | No manifest. The documentation states what the hold covers; the admin decides what to copy. | What to copy depends on the admin's process, from a whole VM image down to one subtree, so SU cannot prescribe it. The coverage scope is three paths and changes approximately never. A pinning test guards against silent drift. |
| 4 | Synchronous commands. No session state machine. | See "Why this is small" below. |
| 5 | Minimal change to `BackupTask`. | Its static helpers already do the right thing. Reuse them rather than refactoring ownership on top of a PR that just stabilised this code. |
| 6 | Config flag, default off for v1. | Not a threat-model argument: anyone with RCON can already `save-off`. It limits the blast radius of a bug in new code to admins who asked for the feature, and many servers do not enable RCON at all until they need it. The default can flip to true once it has proven itself; the flag stays either way. |

## Configuration

All of these are server-side and live with the existing backup config.

| Key | Default | Purpose |
| --- | --- | --- |
| `enable_external_holds` | `false` | Master switch. Flip the default to `true` once the feature has proven itself in the wild; keep the key so admins can still turn it off. |
| `external_hold_default_seconds` | `600` | Lease granted when `begin` is called without an explicit duration. |
| `external_hold_max_seconds` | `1800` | Ceiling. A longer request is clamped to this and the response says so, so the script can shorten its work rather than discover the clamp at expiry. |
| `external_hold_warn_seconds` | `300` | Log a WARN once a hold has been held this long, and periodically after, so a stuck script is visible in the log rather than only at expiry. |
| `external_hold_prepare_timeout_seconds` | `120` | How long a command waits for the server thread before giving up with `TIMEOUT`. Not the lease; it only stops a wedged server from hanging an RCON client forever. |

## Why this is small

`MinecraftServer.handleRConCommand` executes the command inline on the RCON
thread and returns the collected output as the response. It does not queue to
the server thread:

```java
public String handleRConCommand(String command) {
    RConConsoleSource.instance.resetLog();
    this.commandManager.executeCommand(RConConsoleSource.instance, command);
    return RConConsoleSource.instance.getLogContents();
}
```

So a command handler can dispatch work to the server thread, block on a latch,
and return a real answer in one round trip. There is no need for a PREPARING
state, polling for READY, request UUIDs, idempotent retries, terminal-record
history, or an UNKNOWN response.

Two consequences:

- The handler must never touch world state directly. All world mutation goes
  through `ServerUtilitiesServerEventHandler.scheduleServerTask`, which drains
  on `ServerTickEvent`.
- `RConConsoleSource.instance` is a global singleton with a shared
  `StringBuffer`. Two concurrent RCON clients scramble each other's output.
  This is vanilla and cannot be fixed from inside a command. Document it as
  "do not run two backup scripts against one server at the same time".

## Commands

Added under the existing `backup` tree in `CmdBackup`.

```text
backup hold begin [seconds]
backup hold renew <token> [seconds]
backup hold end <token>
backup hold status
```

- `begin` saves players, suspends world saving, drains queued I/O, arms the
  lease, and returns `OK <token> <expires-in-seconds>`. It blocks for as long
  as the save takes, under a hard ceiling.
- `renew` extends the lease. `end` releases it. `status` reports whether a hold
  is active and how long it has left.
- One hold at a time. A second `begin` returns `BUSY`.
- The token exists so a stale script cannot release a hold it no longer owns:
  a script that crashes, whose lease expires, and which then wakes up and sends
  `end` must not release a hold the admin has since started. It is not a secret
  and is not authentication; RCON and console access remain the trust boundary.
- Responses are short single-line plain text with a stable leading token, no
  colour codes and no localised text: `OK`, `BUSY`, `DENIED`, `INVALID_ARGUMENT`,
  `DISABLED`, `EXPIRED`, `NO_HOLD`, `BAD_TOKEN`, `SAVE_FAILED`, `TIMEOUT`.
- `INVALID_ARGUMENT` is separate from `DENIED` so a script author who mistypes a
  duration is not sent to debug RCON permissions.
- `BUSY` also covers a hold that is still being prepared, and a release that has
  not yet confirmed. Neither has a token to report, and both are states a client
  should wait out rather than act on.
- `EXPIRED` covers every way a hold can stop being valid while a script still
  believes it holds one: the lease running out, an admin running `backup stop`,
  and shutdown. The script's correct response is the same in all three cases,
  so distinguishing them on the wire would only invite clients to treat some of
  them as recoverable. `BAD_TOKEN` means the same thing in practice, since it
  arises when a hold was released and another has since started. `NO_HOLD` is
  reserved for `status` against an idle server.

Access: neither `Level.SERVER` nor `Level.OP` is right. `Level.SERVER` requires
`sender instanceof MinecraftServer` and rejects RCON; `RConConsoleSource`
returns `true` from `canCommandSenderUseCommand` for any level, so `Level.OP`
would also admit any opped player. Use an explicit check for console or RCON,
and no in-game players.

## Expiry is the point

If the lease expires mid-copy, SU resumes saving and the copy is torn. The
script must learn this. `end` on an expired hold returns `EXPIRED`, and the
script discards that backup rather than publishing it.

That unambiguous "your backup is invalid" signal is worth as much as the hold
itself. Today the same failure produces a silently corrupt archive.

Corollary for the documentation: never treat a lost connection, a timeout, or
an `EXPIRED` as success. Renewal timing is client-side guidance; only the
server's acknowledgement defines whether the hold still stands.

## Threading and the watchdog

RCON thread: validate arguments, arm the pause mask, submit a `Runnable`, wait
on a latch with a timeout, format the response. Nothing else.

Server thread, inside the `Runnable`: `saveAllPlayerData`, then
`BackupTask.saveAndDisableWorldSaving`, then record the hold and its deadline,
then release the latch. Same ordering the internal backup already uses.

The latch timeout is not the lease. It bounds how long `begin` will wait for
the server thread to answer before giving up with `TIMEOUT`, and exists so a
wedged server cannot hang an RCON client forever. The lease starts once the
hold is granted.

The watchdog is the existing `onServerTick` handler. If a hold is active and
its monotonic deadline has passed, release it and log loudly. No new thread,
and world mutation stays on the server thread where it belongs. Use
`System.nanoTime`, not ticks or wall-clock.

Track "saving is suspended for us" separately from the hold state, and have the
watchdog retry whenever that flag is set with no hold recorded. Without it, a
release whose dispatch never confirms clears the hold, leaves saving suspended
and disarms the very watchdog that should recover it: the server then stops
persisting data silently until someone restarts it. That failure is worse than
anything this feature prevents, so the flag, not the state, is what says
whether saving needs putting back.

Shutdown, normal and crash, releases any hold before the final save. The
existing `MixinMinecraftServer_BackupShutdown` path covers this. A hard process
kill ends the hold with the process; nothing persists across restart.

## The cost of begin

`begin` costs a tick stall. `saveAllChunks` has to run on the server thread,
and on a large world with a lot of dirty chunks that is a visible freeze. It is
the same cost `backup start` already has today, so it is not a regression, but
it must be documented: do not schedule external backups during peak hours and
expect them to be invisible.

The drain, however, does not need the server thread. Once saving is suspended
the world cannot change, and `ThreadedFileIOBase.waitForFinish()` only polls
two volatile counters. So the server thread is released after the save, and the
RCON thread performs the drain wait itself before acknowledging. That keeps the
unavoidable stall down to `saveAllChunks` rather than the save plus the whole
I/O drain, which on a large world is often the longer half. The internal backup
currently waits on the server thread and could adopt the same split later, but
that is not in scope here.

Hodgepodge does not shrink this stall much, and it is worth recording why so
nobody re-litigates it. `AnvilChunkLoader.saveChunk` builds the NBT tree on the
server thread and only then hands the chunk to the I/O thread, which serializes
and compresses it. Every Hodgepodge chunk optimisation, `FastChunkWrite` and
`FastChunkCompression`, sits on the I/O side and so shortens the drain rather
than the stall. `dontSleepOnThreadedIO` is a large win for ambient autosave,
where vanilla sleeps 10ms per queued item, but matters less here because
`waitForFinish` already sets `isThreadWaiting` and zeroes that sleep. The
remaining server-thread cost is inherent: a consistent snapshot means
serializing all dirty state at one instant on the thread that owns it.

## Draining through Hodgepodge when it is present

When `threadedWorldDataSaving` is active, waiting on `ThreadedFileIOBase` alone
is not enough. `WorldDataSaver.flush()` additionally re-queues anything in
`failedData`, retries it, and throws an `IOException` naming the files that
still could not be written. Waiting only on the counters would let SU report a
clean hold over a failed write, which is precisely the failure this feature
exists to eliminate. `MixinCommandSaveAll_WorldDataSave` already wires that
flush into vanilla `save-all`; `begin` should do the same and map a failure to
`SAVE_FAILED`.

Use a `compileOnly` dependency on Hodgepodge with `{transitive = false}`,
matching the house style for every other soft dependency in
`dependencies.gradle`, rather than reflection.

This does not create a dependency cycle even though Hodgepodge already depends
on SU. That edge is `runtimeOnlyNonPublishable` for the pregenerator, and
`compileOnly` does not appear in a published POM either, so neither direction
propagates. The practical consequence is only that the two cannot be bumped in
one atomic step: SU pins a released Hodgepodge version, so this waits on the
save-corruption PR merging and being released.

Guard the call with `Loader.isModLoaded("hodgepodge")` through `OtherMods`, and
keep every reference to `WorldDataSaver` inside its own small class that is
only touched after that check passes. Referencing the type directly from a
class that loads unconditionally will fail verification on a server without
Hodgepodge, which is the usual way this pattern goes wrong.

## Pause-when-empty

This is the one real trap. `MixinMinecraftServer_PauseWhenEmpty` cancels
`tick()` at `HEAD` while paused, so `ServerTickEvent` never fires and anything
dispatched to the server thread hangs forever. External backups run at night,
when the server is empty and paused, so this is the normal case rather than an
edge case.

The existing `serverUtilities$setPauseWhenEmptyMaskSeconds` mask looks like the
answer but is the wrong shape for this. It is a countdown, so it has to be
re-armed every tick and nothing clears it on release, leaving the server awake
for the remainder of the last armed lease after a snapshot that took seconds.
It is also the same field `CmdPauseWhenEmptyMask` writes, so a hold would
silently overwrite an administrator's own mask twenty times a second.

Inhibit instead. The mixin's `@Inject` is at `tick` HEAD and runs even while
paused, so adding `!ExternalBackupHold.INSTANCE.isHeld()` to the pause condition
wakes the server on the very next tick, needs no countdown, no cross-thread
field write, no per-tick re-arming, and leaves no residue when the hold ends.
Because `isHeld()` also covers preparation and an unconfirmed release, the
server stays awake for exactly as long as the work needs it.

## Interaction with the internal backup

- `begin` while `BackupTask.isBackupRunning()` returns `BUSY`.
- A scheduled automatic backup that fires during a hold skips that occurrence
  and logs at WARN. Skipping a scheduled backup silently would be a quiet
  data-protection regression, so it must be visible in the log.
- `CmdBackupStart` during a hold reports busy rather than starting.
- `backup stop` releases an external hold, for the case where a hold is stuck
  and the admin needs saving back immediately. No confirmation prompt: an admin
  typing it is being deliberate, and the failure is safe because the script
  learns its hold is gone and discards that backup. Note that `CmdBackupStop`
  is `Level.OP_OR_SP`, which is permission level 2 rather than 4, so any op can
  do this and not only a console operator. That is acceptable given the safe
  failure, but it should be in the admin documentation.
- The existing `save-all` / `save-on` / `save-off` guard keys off
  `BackupTask.isWorldSavingSuspended()`, so it covers external holds for free.
  Its current message is `cmd.backup_already_running`, which will read wrong
  during an external hold; add a separate lang key.

Because SU stages nothing here, the external path never touches
`serverutilities/temp/`, and does not interact with the wholesale
`FileUtils.delete(BACKUP_TEMP_FOLDER)` in `BackupTask.postBackup`.

## What the hold covers

SU does not tell the admin what to copy. That is their decision and it depends
entirely on their process: a VM-level backup copies a whole disk, an rsync job
copies a subtree, a snapshot covers a filesystem. Prescribing a file list would
be wrong for most of them.

What SU states instead is the scope of the guarantee, so an admin can tell
which parts of whatever they copied are consistent:

- the world save directory, `DimensionManager.getCurrentSaveRootDirectory()`,
  which already contains `<world>/serverutilities/`
- `serverutilities/server/ranks.txt`
- `serverutilities/server/players.txt`

Anything else in the copy is outside the guarantee. That is not a problem for
files that do not change at runtime, such as `config/` and `mods/`, and the
documentation should say so rather than leaving admins to wonder.

Both global files resolve from `ServerUtilities.SERVER_FOLDER`. Add a test
pinning that constant so moving the files fails the build and points whoever
moved them at this document.

This is also why the earlier draft's manifest made no sense here. A manifest
with `source` and `target` entries prescribes how to assemble an archive with a
particular layout, which only has meaning if SU is producing the archive. It
has none for someone snapshotting a volume.

## Stages

### 1. Hold owner, commands, watchdog

Hold state, lease and clamping, token checks, pause-mask arming, tick watchdog,
shutdown release, access check, config. Reuse the existing static helpers in
`BackupTask` rather than refactoring ownership.

Tests use controlled time and injected I/O failures, never sleeps: competing
`begin` calls, `end` with a stale token, expiry during a hold, renew against an
already-expired deadline, save failure during `begin`, release on shutdown,
automatic backup skipped during a hold, and the path-constant pin. Preserve
every regression from the parent PR.

### 2. Live validation on a dedicated server

The RCON response path and the server-thread bridge cannot be inferred from
mocked unit tests. Verify on a real dedicated server, with and without
Hodgepodge: a hold on an empty paused server, a hold alongside autosave and
player logout, dimension load and unload during a hold, vanilla save commands
during a hold, script killed mid-hold, `backup stop` during a hold, and both
normal and crash shutdown. Record tested Hodgepodge versions.

Each failure mode must produce either correct captured bytes or an explicit
rejection. No silent partial backups.

### 3. Documentation and example scripts

Admin-facing usage documentation: what the hold covers and what it does not,
the guarantee and its limits, the tick-stall cost, the response codes, the rule
that expiry means discard, and the one-script-at-a-time caveat. Example scripts
for the two shapes that matter, a filesystem snapshot and a plain copy, written
to show the lifecycle rather than to be a product. Update the README.

## Not doing

Native VSS, Btrfs, ZFS or LVM integration. Remote file streaming over RCON or
any other transport. HTTP, a daemon, or a service installer. A new archive
format, upload, or retention logic. A universal mod write barrier. Claimed-only
external captures. Integrated server, Bukkit or Thermos support. A server-side
restore command; restoring an external backup is manual file placement on a
stopped server, and the documentation should say so plainly.

## Still open

- The minimum Hodgepodge version to pin and to recommend. Blocked on the
  save-corruption PR merging and being released. Until then, stage 1 can be
  written against the local branch and the version filled in later.
- Whether the documentation should state that Hodgepodge is effectively
  required for the strong form of the guarantee, or present it as a
  recommendation. The honest position is closer to the former on a server that
  cares enough to run external backups at all.
