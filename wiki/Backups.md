# Backups

ServerUtilities creates ZIP backups of the world on a timer and deletes older ones according to your retention
settings. Optionally, backups can be stored [incrementally](#incremental-backups-experimental) instead. These are two separate things: `backup_timer` decides **how often** a backup is created, retention decides
**which** existing backups are kept.

## Settings

All options are in the `backups` section of `serverutilities/serverutilities.cfg`.

**When backups are made**

| Option | Default | Description |
| --- | --- | --- |
| `enable_backups` | `true` | Enables automatic backups. Manual backups always work. |
| `backup_timer` | `30m` | Time between backups, e.g. `5m`, `1h`, `1d` (see [units](#duration-units)). Minimum `1s`. |
| `need_online_players` | `true` | Skip automatic backups while the server is empty. A final backup still runs after the last player leaves. Set to `false` if automation keeps changing the world while nobody is online. |

**Which backups are kept**

| Option | Default | Description |
| --- | --- | --- |
| `retention_policy` | empty | Age-based rules, see [policy mode](#policy-mode-keep-history-by-age). Empty uses count/size mode. |
| `backups_to_keep` | `12` | Count/size mode: number of backups to keep. Ignored in policy mode. |
| `max_folder_size` | `0` | Size allowance in GB for rotating backups, see [size limit](#what-is-always-kept-and-how-the-size-limit-works). `0` disables it. |
| `delete_custom_name_backups` | `true` | Set to `false` to never delete backups created with a custom name. |

**What goes into a backup**

| Option | Default | Description |
| --- | --- | --- |
| `backup_format` | `ZIP` | `ZIP`, or `INCREMENTAL` for [incremental backups](#incremental-backups-experimental) (experimental). |
| `backup_folder_path` | `./backups/` | Where backups are stored. |
| `compression_level` | `1` | `0` uncompressed, `1` fastest, `9` smallest. |
| `additional_backup_files` | NEI data | Extra paths to include. Use `/`, `*` wildcards and `$WORLDNAME`; end folders with `/**`. |
| `excluded_backup_files` | empty | Paths to exclude (region files are never excluded). Same syntax. |
| `only_backup_claimed_chunks` | `false` | Only back up claimed chunks. Much smaller, but unclaimed chunks cannot be restored. |
| `backup_entire_regions_with_claims` | `false` | With the option above, keep whole region files that contain a claim. |

**Performance and messages**

| Option | Default | Description |
| --- | --- | --- |
| `use_separate_thread` | `true` | Write the ZIP in the background (recommended). |
| `prefer_speed_over_backup_consistency` | `false` | Less lag when a backup starts, but non-chunk data may be copied while it changes. |
| `display_file_size` | `true` | Show backup and folder size when a backup finishes. |
| `silent_backup` | `false` | Hide all backup messages except critical errors. |

### Duration units

Durations are a positive whole number followed by `s` (seconds), `m` (minutes), `h` (hours), `d` (24 hours) or
`w` (7 days). `m` never means months: use `30d`.

Older configs stored `backup_timer` as a number of hours. These values are converted on startup, e.g. `0.5` becomes
`30m` and `24` becomes `1d`. An invalid timer is reported in the log and disables automatic backups until fixed.

## Commands

All commands require operator permissions, or singleplayer.

| Command | Description |
| --- | --- |
| `/backup start [name] [=oc] [=overwrite]` | Start a backup now. `=oc` only includes claimed chunks. A name that already exists is refused unless `=overwrite` is given. |
| `/backup stop` | Cancel the running backup. |
| `/backup getsize` | Show the world size and the backup folder size. |
| `/backup list` | List backups, oldest first, with their size and age. |
| `/backup prune preview` | Show, oldest first, which backups policy mode would keep or delete, the rule responsible and how long finite rules keep each one. Nothing is deleted. |

`list` and `prune preview` run in the background and report "busy" while a backup is running.

Singleplayer backups can also be restored from the **Backups** button on the world selection screen.

## Count/size mode (default)

With an empty `retention_policy`:

- `backups_to_keep` keeps the newest backups up to that count.
- If `max_folder_size` is set, it replaces the count: the oldest backups are deleted until the rest fit.
- Backups are ordered by file modification time, across all worlds, but the newest backup of each world is
  always kept, even if that means going over the limit.

## Policy mode: keep history by age

Policy mode keeps recent backups often and older ones at wider intervals. Example:

```properties
S:retention_policy <
    1h:all
    1d:30m
    7d:1h
    30d:1d
    forever:1w
>
```

Each rule is `max age:interval`. For each world, it keeps the newest backup in every interval within that age:

| Rule | What it keeps |
| --- | --- |
| `1h:all` | Every backup from the last hour |
| `1d:30m` | One backup per 30 minutes for the last day |
| `7d:1h` | One backup per hour for the last 7 days |
| `30d:1d` | One backup per day for the last 30 days |
| `forever:1w` | One backup per week, forever |

- A backup selected by any rule is kept. Rule order does not matter.
- Ages use real elapsed time. Periods without backups stay empty, for example while the server is offline or empty.
- The current period's backup is replaced by newer ones until that period ends.
- An invalid rule disables cleanup (with a log warning) until it is fixed. Backups are still created.

Use `/backup prune preview` to check a policy before relying on it.

## What is always kept, and how the size limit works

In both modes, cleanup never deletes:

- the latest backup of each world,
- custom-named backups when `delete_custom_name_backups=false`,
- ZIPs it cannot read or recognize as a world backup,
- backups dated in the future,
- in policy mode, backups selected by a `forever` rule,
- other files, folders and symbolic links in the backup folder.

**`max_folder_size` is an allowance for rotating backups, not a hard cap on the folder.** Protected custom backups,
`forever` backups, and unreadable or future-dated ZIPs do not count toward it. For example, a 5 GB allowance plus
8 GB of `forever` history can use 13 GB in total.

In policy mode, age rules run first. If the counted backups still exceed the allowance, the oldest backups kept only
by finite rules are deleted, so **a small size limit can shorten your configured history**. A warning is logged if
the allowance cannot be met. Backups are never stopped because of the allowance, so keep an eye on free disk space.

## Incremental backups (experimental)

With `backup_format=INCREMENTAL`, backups go into a deduplicating repository in `<backup_folder_path>/incremental/`
instead of separate ZIP files. It works like [Borg](https://www.borgbackup.org/):

- Files are split into chunks and each chunk is stored once, identified by its SHA-256 hash. A backup only adds the
  chunks that changed since earlier backups; everything else is shared. Region files are split where Minecraft's own
  chunks start, so a region with a few changed chunks only adds those.
- Files whose size and modification time have not changed since the last backup are not read again, so backing up a
  large, mostly unvisited world takes a fraction of the time.
- Chunks are compressed with `compression_level` and kept in pack files in `data/`. Each backup is a small file in
  `archives/`, named like the ZIP backups.
- Every chunk is verified against its hash when it is read back.

Every incremental backup is a complete snapshot: restoring one never needs the others, and deleting one only frees the
data no other backup uses.

**Restoring.** In singleplayer, incremental backups appear on the Backups screen next to ZIP backups. There, and on
servers, they are restored with the tool built into the ServerUtilities jar, which runs without Minecraft; the
Backups screen uses the same restore code. Stop the server first, then from the server folder:

```sh
java -jar mods/ServerUtilities-<version>.jar list
java -jar mods/ServerUtilities-<version>.jar restore latest restored/
```

| Command | Description |
| --- | --- |
| `list` | List backups, oldest first, and the repository's size on disk. |
| `info <archive>` | Details of one backup, including how much space deleting it would free. |
| `files <archive> [glob]` | List the files in a backup. |
| `restore <archive> <target>` | Restore into a folder. Refuses to replace existing files unless told how: `--replace-world` moves an existing world folder to `<world>_old` first, and `--overwrite` replaces other files, moving them to `<target>/backups_before_restore/`. `--world-only` skips global files, `--include <glob>` restores only matching paths (e.g. `'World/region/*'`), `--dry-run` only lists. If anything fails, the files written so far are removed and everything moved aside is put back. |
| `export <archive> <file.zip>` | Convert a backup to a regular ZIP backup. |
| `check [--verify-data]` | Check that every backup is complete; `--verify-data` also reads and verifies all stored data. |
| `delete <archive>...` | Delete backups and free the space only they used. |

`<archive>` is a name from `list` or `latest`. Paths in a backup are relative to the server folder, so
`restore latest . --replace-world --overwrite` puts the world back where it was, the same way the Backups screen's
"Restore (with global data)" does; "Restore" corresponds to adding `--world-only`. Use `--repo <folder>` if the repository is elsewhere. The reading commands also work while the server runs,
and wait up to `--wait` seconds (default 60) for a backup in progress to finish.

**Retention.** The same retention settings apply, but separately from ZIP backups. Since backups share data,
`max_folder_size` counts the deduplicated space backups actually use, and deleting a backup frees only what no other
backup needs. Freed space is reclaimed right after pruning by rewriting pack files that became mostly unused.

**Limitations of this first version.** Backups are not encrypted. The chunk index is held in memory during a backup,
roughly 100 MB per 50 GB of world. Never edit or delete files in the repository by hand: removing one pack file damages
every backup that uses it. Use `check` if in doubt.

## When cleanup runs

Cleanup runs in the background at server start and after each successful backup. Failed or cancelled backups do not
trigger it. Failed deletions are logged and skipped.

<details>
<summary>Technical details</summary>

### Time periods

Periods are aligned to UTC: minute, hour and day intervals start on UTC boundaries, and `1w` starts on Monday.
Other intervals count from Monday 1970-01-05 00:00 UTC. Under an unchanged policy, a completed `forever` period
keeps its backup. Changing the policy or deleting files by hand can remove that history.

### Backup metadata and older backups

New backups contain a small `.serverutilities-backup.properties` file with the world UUID, creation time and
whether the backup has a custom name. It is ignored when restoring.

Older backups without this file are identified by:

- **World:** the UUID in `serverutilities/universe.dat` inside the ZIP, otherwise the world folder name.
- **Time:** the timestamp in the filename (server local time), otherwise the file modification time.
- **Custom name:** whether the filename is a timestamp.

Changing timezone or touching these older files can change how they are sorted.

Worlds cloned with the same UUID share retention history if their backups are in the same folder.

### Recognized archives

A ZIP is treated as a world backup only if it contains a non-empty `level.dat` or `level.dat_old` in exactly one safe
world folder, such as `<world>/` or `saves/<world>/`. If the ZIP has a comment, the folder name must match it.
Anything else is left untouched.

### Custom names

Custom names cannot contain path separators, control characters or reserved filename characters. With
`=overwrite`, the existing backup stays in place until the new one is complete. Automatic backups created in the
same second get a numbered suffix instead of replacing each other.

</details>
