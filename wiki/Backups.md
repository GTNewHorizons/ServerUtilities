# Backups

ServerUtilities creates complete ZIP backups and removes older ones according to your retention settings.
Backup frequency and retention are separate: `backup_timer` controls when backups are created; retention controls
which existing backups survive cleanup.

`backup_timer` uses the same duration units as retention rules. For example, `S:backup_timer=5m` creates a backup
every five minutes. The default is `30m`. Use a positive integer followed by `s` (seconds), `m` (minutes),
`h` (hours), `d` (24 hours), or `w` (seven days); the minimum is `1s`. `all` and `forever` are only for retention.
Frequent backups and long retention can use considerable disk space.

Existing values without units are still interpreted as hours and automatically saved with units on startup.
Migration rounds up to a whole second, then uses the largest unit that expresses that duration exactly:
`0.5` becomes `30m`, `1.5` becomes `90m`, `24` becomes `1d`, and `0.0834` becomes `301s`.
Legacy zero becomes `1s`; disable backups with `enable_backups=false` instead.
Invalid or overflowing timers are reported in the log and prevent automatic backups until corrected.
Manual backups remain available, and retention settings are unaffected.

With `need_online_players=true` (the default), automatic backups run while players are online or player activity
is pending. Login and logout activity permits a final scheduled backup after everyone leaves. Failed or cancelled
attempts leave that activity pending for another attempt; activity during an asynchronous backup remains pending
even if that backup succeeds. A successful manual backup also covers activity recorded before it started.

This setting applies in both retention modes. Policy windows use elapsed time: periods without backups remain
empty, and finite history can age out while the server is empty, although the latest eligible backup is protected.
Set `need_online_players=false` for backups while empty, especially if automation keeps changing the world.

## Upgrade notes

Custom-name reuse now requires an explicit overwrite: change scripts using `/backup start checkpoint` repeatedly
to `/backup start checkpoint =overwrite`. Without this flag, an existing archive is preserved and the command
reports an error. `=overwrite` can be combined with `=oc` and requires a custom name.

## Choose a retention mode

| You want to... | Settings to use |
| --- | --- |
| Keep a fixed number of backups | Leave `retention_policy` empty, set `max_folder_size=0`, and use `backups_to_keep` |
| Limit the space used by rotating backups | Leave `retention_policy` empty and set `max_folder_size` to a positive number of GB |
| Keep recent backups frequently and older history less frequently | Set `retention_policy` using the example below |

## Default mode: keep by count or size

With an empty `retention_policy`, ServerUtilities uses **count/size retention** (called legacy retention in logs):

- **Count:** `backups_to_keep=12` keeps the newest 12 eligible backups.
- **Size:** a positive `max_folder_size` removes older eligible backups until they fit the limit. This overrides
  `backups_to_keep`.
- **Named backups:** set `delete_custom_name_backups=false` to protect custom-named backups. Protected files do not
  count toward the legacy count or size limit.

Both retention modes use new archives' metadata to identify custom names, including names that look like timestamps.
Archives without metadata use the original filename-based detection. Invalid metadata is preserved rather than
falling back to the filename.

Only recognized world backups in regular `.zip` files directly in the backup folder are eligible. They are ordered
by file modification time, with filename breaking ties. The newest eligible archive **for each world** is always kept.
Count and size limits apply across all worlds, but can be exceeded to preserve those last backups. Unreadable,
unrecognized and future-dated ZIPs are preserved and excluded from the legacy count and size limits.

## Policy mode: keep history by age

Populate `retention_policy` to keep recent backups frequently and older backups at wider intervals:

```properties
S:retention_policy <
    1h:all
    1d:30m
    7d:1h
    30d:1d
    forever:1w
>
```

For each world, this example keeps:

| Rule | What it keeps |
| --- | --- |
| `1h:all` | Every backup from the last hour |
| `1d:30m` | The newest backup in each 30-minute period within the last day |
| `7d:1h` | The newest backup in each hourly period within the last seven days |
| `30d:1d` | The newest backup in each daily period within the last 30 days |
| `forever:1w` | The newest backup in each weekly period, with no age limit |

Each rule is `maximum age:sampling interval`. Durations must be positive integers followed by `s` (seconds),
`m` (minutes), `h` (hours), `d` (24 hours), or `w` (seven days). Use `30d` for 30 days; `m` never means months.
`all` keeps every backup in the age window; `forever` removes the age limit.

Rules combine: a backup selected by any rule is kept. Their order does not matter, and overlap does not create
duplicate files. `backups_to_keep` is ignored in policy mode.

These are fixed time periods, not a promise of exact spacing between backups. Periods with no backups remain empty.

## Preview a policy before cleanup

As an operator, or in singleplayer, run:

```text
/backup prune preview
```

The preview lists which archives would be kept or deleted and why, without deleting anything. It uses the same
selection rules as automatic cleanup. Its summary shows both the total remaining ZIP size and the size counted
against the rotation allowance. An empty policy reports that legacy retention is active.

## What a policy always protects

Policy cleanup preserves these archives, even when the size limit cannot be met:

- The latest eligible backup for each world, even if older than every finite age window.
- Backups selected by a `forever` rule.
- Custom-named backups when `delete_custom_name_backups=false`.
- Unrecognized or unreadable ZIPs, which do not participate in world or time-period selection.
- Archives with timestamps in the future.

Protected custom backups are kept separately: they do not replace the latest automatic backup or fill policy buckets.
When `delete_custom_name_backups=true`, custom backups participate in selection normally.

If `max_folder_size` is positive, age cleanup runs first. Protected custom backups, unrecognized/unreadable ZIPs,
future-dated archives and selected `forever` representatives are excluded from this rotation size allowance,
including backups selected by both finite and `forever` rules. If the remaining counted ZIPs still exceed the allowance,
ServerUtilities removes the oldest backups selected only by finite-age rules. **The size limit can shorten your
configured history.** The latest eligible backup per world still counts unless otherwise excluded, but remains
protected. An unmet allowance produces a warning.

`max_folder_size` does not cap the total folder size or stop backup creation. For example, a 5 GB rotation allowance
plus 8 GB of protected custom backups or `forever` representatives can use 13 GB in total. Permanent history can
continue growing, and backups can fail if the disk runs out of space. All archives stay in the same folder.

Both modes leave unrelated files, directories, symbolic links and active archive staging files alone.

## Cleanup and errors

Cleanup runs at startup and after a backup is successfully published. Failed or cancelled backups do not trigger
post-backup cleanup. Failed deletions do not count as freed space or removed backups.

Both modes scan archives and delete backups in the background. World saving resumes before post-backup pruning starts.
Manual backups and further previews report busy while the worker is running. Scheduled backups retry after one second
when only a retention scan is busy. If another backup is running or preparing, they wait the normal configured
interval instead of catching up immediately after it finishes. A cleanup request made during a scan runs
when that scan finishes; repeated requests are combined. `/backup stop` and server shutdown
cancel the worker and wait for it to stop.

| Situation | What happens |
| --- | --- |
| A policy rule is invalid | The entire policy is rejected and pruning is skipped. Backups can still be created; fix the rule to resume cleanup. |
| A scan finds an unreadable or unrecognized ZIP | The ZIP is preserved and reported in the log. |
| A deletion fails | The failure is logged; size cleanup tries the next eligible older backup without deleting protected archives. |
| A count or size limit cannot be met | A warning reports the remaining eligible count or counted size. Protected custom backups, unrecognized/unreadable ZIPs, future-dated archives and selected `forever` representatives are excluded from the size allowance. |
| Retention settings change during a scan | Its deletion plan is skipped. |

Custom backup names must be filenames without path separators, control characters or reserved filename characters.
They cannot place archives outside the backup folder. Existing directory or symlink targets are rejected.
Reusing a custom name is rejected before preparing the backup. To intentionally replace a checkpoint, run
`/backup start checkpoint =overwrite`. This can be combined with `=oc`; `=overwrite` requires a custom name.
The existing archive remains until the replacement has finished writing. Automatic timestamp collisions receive
a numbered suffix, so two backups created in the same second can coexist. The launch reply is sent only when
preparation starts a worker or a synchronous backup finishes successfully.

<details>
<summary>Technical details and compatibility with older backups</summary>

### Time periods and permanent history

Policy periods use UTC boundaries. Standard minute, hour and day intervals align to UTC clock boundaries;
`1w` starts Monday. Other intervals use the same origin: Monday 1970-01-05 at midnight.

A newer backup can replace the selected archive in the current period, including a `forever` period.
Under an unchanged policy, completed `forever` periods retain their selected archives during normal backup creation.
Changing the policy or manually deleting backups can remove that history.

### Identifying worlds and backup times

New backups store their creation timestamp, world UUID and custom-name status inside the ZIP. This metadata is
excluded from restoration.

Both modes use saved world UUIDs when available, otherwise the archive's recognized world-folder name.
Archives without a UUID are grouped by world-folder name. Older timestamp filenames are interpreted in the server's
local timezone; older custom-named archives use file modification time. Changing timezone or touching these older
files can affect policy selection.

Cloned worlds that keep the same UUID share retention history when their backups are in the same folder.

### ZIP checks

To participate in either retention mode, an archive must contain a nonempty `level.dat` or `level.dat_old` in exactly
one safe world folder. When a ZIP comment is present, the folder's final name must match it. Without a comment,
the unique world folder identifies the world. This includes `<world>/`, `saves/<world>/`, and nested folders such as
`worlds/<world>/`. Unsafe paths or multiple candidate world folders are rejected. Archives
that fail this check are preserved and cannot replace the latest recognized backup.

Both modes validate backup metadata when present, or read the world UUID from `serverutilities/universe.dat`
when available in an older archive. These are archive structure checks, not full payload-integrity checks.

### Command replies and invalid legacy limits

Player and console preview replies arrive on a later server tick. RCON waits on its own thread for the complete reply.

Legacy counts below one are treated as one. A negative legacy size limit skips pruning.

</details>
