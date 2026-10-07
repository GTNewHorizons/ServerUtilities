# Backup retention

By default, ServerUtilities keeps backups using `backups_to_keep`, or `max_folder_size` when it is nonzero.
An empty `retention_policy` preserves that behavior. Populate this single list to enable age-window retention instead:

```properties
S:retention_policy <
    1h:all
    1d:30m
    7d:1h
    30d:1d
    forever:1w
>
```

Each entry is `maximum age:sampling interval`. Positive integer durations accept `s` (seconds), `m` (minutes),
`h` (hours), `d` (24 hours), and `w` (7 days). Use `30d` for a 30-day window; `m` never means months.
`all` keeps every backup in the window. `forever` removes the age limit.

The example keeps every backup from the last hour, one per 30-minute bucket within the last day, one per hour
within seven days, one per day within 30 days, and one per week indefinitely. Rules combine as a union;
their order does not matter and overlapping selections do not create copies. `backups_to_keep` is ignored.

Retention does not create backups. Keep using `backup_timer` (in hours) to set the creation frequency.
For example, `S:backup_timer=0.08333333333333333` schedules backups every five minutes.
Backups remain complete ZIP archives, so frequent backups and long retention can consume considerable disk space.

## Time buckets and permanent history

Buckets have fixed UTC boundaries, anchored to Monday 1970-01-05 at midnight. Standard minute/hour/day intervals
align to UTC clock boundaries and `1w` starts Monday. Other intervals use the same fixed origin.
The newest backup in each populated bucket is selected. This is sampling by bucket, not a guarantee of exact
spacing between retained backups. Missing periods, including offline time, are not filled.

The latest backup for each world is always preserved, even when older than every finite window.
The current bucket's representative can be replaced by a newer backup. Completed `forever` buckets retain their
representative under an unchanged policy. This preserves historical snapshots; it does not pin every archive
temporarily selected during the current week. Changing the policy or manually deleting backups can remove history.

New backups record their creation timestamp, world UUID, and whether they were custom-named inside the ZIP.
This metadata is excluded from restoration. Existing archives use their saved world UUID when available,
otherwise their ZIP world-folder comment. Legacy timestamp filenames are interpreted in the server's local timezone;
legacy custom-named archives use their file modification time. Changing timezone or touching legacy custom archives
can affect their classification. Archives lacking a UUID are grouped by their world-folder name.

## Folder size and protected backups

With a policy enabled, age pruning runs first. If `max_folder_size` is nonzero and recognized/protected ZIPs still
exceed that size, finite-retention backups are removed oldest first. This can shorten the configured age windows.

The latest backup per world, representatives selected by `forever` rules, and custom-named backups protected by
`delete_custom_name_backups=false` are preserved regardless of the size limit. Unrecognized/unreadable ZIPs and
future-dated archives are also preserved. If these protections prevent meeting the limit, SU logs a warning.
Archives must contain a nonempty `level.dat` or `level.dat_old` matching their world-folder comment in a single
supported layout before they can participate in retention. ZIPs missing those entries are preserved and cannot
replace the latest recognized backup. This is a structural check, not a full payload-integrity check.
Unrelated files, directories, symbolic links, and active archive staging files are excluded from policy pruning.

## Preview and errors

Use `/backup prune preview` as an operator (or in singleplayer) to inspect keep/delete decisions and their reasons
without deleting backups. It uses the same selection logic as automatic cleanup. An empty policy reports that
legacy retention is active.
Policy scans and deletion run on a background worker. New backups and further previews report busy while it is
running; world saving is restored before post-backup pruning starts. Player/console preview replies arrive on a
later server tick, while RCON waits on its own thread for the complete reply. `/backup stop` and server shutdown
cancel and join the worker. If retention settings change during a scan, its deletion plan is skipped.

Invalid rules reject the entire policy and skip pruning; backups can still be created. Fix the config before cleanup
resumes. Unreadable/unrecognized ZIPs are kept and reported rather than guessed at. Cleanup runs on server startup and
after successfully publishing a backup; failed or cancelled backups do not trigger post-backup pruning.

Archive deletion errors are logged and remaining size is checked against the files actually left on disk.

## Legacy retention and custom names

With an empty policy, legacy retention considers only regular `.zip` files directly in the backup folder.
Directories, symbolic links, unrelated files and archive staging files are never pruned. If custom backups are
protected, their files do not count toward the legacy count or size limit. Size retention takes precedence over
count retention when `max_folder_size` is positive.

Legacy retention sorts eligible files by filesystem modification time, breaking ties by filename. It always
preserves the newest eligible archive, even if that archive alone exceeds the size limit; a count below one is
treated as one. Failed deletions do not count toward either limit, and an unmet limit is logged. A negative size
limit skips pruning. Legacy selection is global and filename-based: it does not inspect ZIP contents or preserve
the newest backup separately for each world. Use an age-window policy for world-aware selection.

Custom backup names must be filenames without path separators, control characters or reserved filename characters.
Names cannot place archives outside the backup folder, and publication rejects existing directory or symlink targets.
