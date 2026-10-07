# Backups

ServerUtilities creates complete ZIP backups and removes older ones according to your retention settings.
Backup frequency and retention are separate: `backup_timer` controls when backups are created; retention controls
which existing backups survive cleanup.

`backup_timer` is measured in hours. For example, `S:backup_timer=0.0834` creates a backup roughly every five minutes.
Frequent backups and long retention can use considerable disk space.

## Choose a retention mode

| You want to... | Settings to use |
| --- | --- |
| Keep a fixed number of backups | Leave `retention_policy` empty, set `max_folder_size=0`, and use `backups_to_keep` |
| Limit the space used by backups | Leave `retention_policy` empty and set `max_folder_size` to a positive number of GB |
| Keep recent backups frequently and older history less frequently | Set `retention_policy` using the example below |

## Default mode: keep by count or size

With an empty `retention_policy`, ServerUtilities uses **legacy retention**:

- **Count:** `backups_to_keep=12` keeps the newest 12 eligible backups.
- **Size:** a positive `max_folder_size` removes older eligible backups until they fit the limit. This overrides
  `backups_to_keep`.
- **Named backups:** set `delete_custom_name_backups=false` to protect custom-named backups. Protected files do not
  count toward the legacy count or size limit.

Only regular `.zip` files directly in the backup folder are eligible. They are ordered by file modification time,
with filename breaking ties. The newest eligible archive is always kept, even if it alone exceeds the size limit.

**Limitation:** legacy retention treats the folder as one collection and does not check ZIP contents. A newer
malformed ZIP can displace usable history, and there is no separate "latest backup" protection for each world.
Use policy mode for ZIP structure checks and protection for each world.

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
selection rules as automatic cleanup. An empty policy reports that legacy retention is active.

## What a policy always protects

Policy cleanup preserves these archives, even when the size limit cannot be met:

- The latest recognized backup for each world, even if older than every finite age window.
- Backups selected by a `forever` rule.
- Custom-named backups when `delete_custom_name_backups=false`.
- Unrecognized or unreadable ZIPs, which do not participate in world or time-period selection.
- Archives with timestamps in the future.

If `max_folder_size` is positive, age cleanup runs first. If the remaining ZIPs still exceed the limit,
ServerUtilities removes the oldest backups selected only by finite-age rules. **The size limit can shorten your
configured history.** Protected archives remain, and an unmet limit produces a warning.

Both modes leave unrelated files, directories, symbolic links and active archive staging files alone.

## Cleanup and errors

Cleanup runs at startup and after a backup is successfully published. Failed or cancelled backups do not trigger
post-backup cleanup. Failed deletions do not count as freed space or removed backups.

Policy scans and deletion run in the background. World saving resumes before post-backup pruning starts.
New backups and further previews report busy while the worker is running. `/backup stop` and server shutdown
cancel the worker and wait for it to stop.

| Situation | What happens |
| --- | --- |
| A policy rule is invalid | The entire policy is rejected and pruning is skipped. Backups can still be created; fix the rule to resume cleanup. |
| A policy scan finds an unreadable or unrecognized ZIP | The ZIP is preserved and reported in the log. |
| A deletion fails | The failure is logged; cleanup checks whether the limit can still be met. |
| A count or size limit cannot be met | A warning reports the remaining eligible count or size. In policy mode, protected ZIPs also count toward size. |
| Retention settings change during a policy scan | Its deletion plan is skipped. |

Custom backup names must be filenames without path separators, control characters or reserved filename characters.
They cannot place archives outside the backup folder. Existing directory or symlink targets are rejected.

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

In policy mode, older archives use their saved world UUID when available, otherwise their ZIP world-folder comment.
Archives without a UUID are grouped by world-folder name. Older timestamp filenames are interpreted in the server's
local timezone; older custom-named archives use file modification time. Changing timezone or touching these older
files can affect policy selection.

### ZIP checks

To participate in policy selection, an archive must contain a nonempty `level.dat` or `level.dat_old` matching its
world-folder comment in exactly one supported layout: `<world>/` or `saves/<world>/`. Archives that fail this check
are preserved and cannot replace the latest recognized backup.

This checks archive structure, not full payload integrity.

### Command replies and invalid legacy limits

Player and console preview replies arrive on a later server tick. RCON waits on its own thread for the complete reply.

Legacy counts below one are treated as one. A negative legacy size limit skips pruning.

</details>
