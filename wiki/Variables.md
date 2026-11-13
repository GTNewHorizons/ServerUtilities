# Variables

The MOTD (`motd.line1`, `motd.line2`) and the header and footer of the modern tab list (`tab.headerText`,
`tab.footerText`) can contain variables in curly brackets. The server replaces them with the current value: in the MOTD
every time the server is pinged, in the tab list once a second.

```
S:headerText=&b&lMy Server\n&7{players}/{maxPlayers} online
S:footerText=&7TPS {tpsColor}{tps} &7MSPT {msptColor}{mspt}&7 Ping {pingColor}{ping}ms
```

Names are case-sensitive. Text in curly brackets that is not one of the names below is left as it is.

## About the server

These work in the MOTD and in the tab list.

| Variable | Value | Example |
|---|---|---|
| `{players}` | Players online | `3` |
| `{maxPlayers}` | Player limit | `20` |
| `{uniquePlayers}` | Players who have ever joined | `15` |
| `{tps}` | Ticks per second over the last 100 ticks, at most 20 | `19.8` |
| `{mspt}` | Milliseconds per tick over the last 100 ticks | `12.4` |
| `{tpsColor}` | A color code for the TPS: green from 19, yellow from 15, red below | |
| `{msptColor}` | A color code for the MSPT: green up to 40, yellow up to 50, red above | |
| `{memory}` | Used and maximum memory | `5321/12288MB` |
| `{memoryUsed}` | Used memory in MB | `5321` |
| `{memoryMax}` | Maximum memory in MB | `12288` |
| `{memoryPercent}` | Used memory in percent of the maximum | `43` |
| `{uptime}` | Time since the server started | `2d 4h` |
| `{entities}` | Loaded entities in all dimensions | `1284` |
| `{tileEntities}` | Loaded tile entities in all dimensions | `9120` |
| `{chunks}` | Loaded chunks in all dimensions | `1756` |
| `{dimensions}` | Loaded dimensions | `7` |
| `{time}` | Time of day on the server machine | `21:05` |
| `{date}` | Date on the server machine | `2026-11-13` |
| `{day}` | Day of the overworld, starting at 1 | `238` |
| `{worldTime}` | Clock of the overworld | `07:30` |

A color variable only sets the color. Put it right before the value, and the color you want for the rest after it:
`{tpsColor}{tps}&7 TPS`.

## About the player

These are about the player looking at the tab list, so they only work there. In the MOTD they are left as they are.

| Variable | Value | Example |
|---|---|---|
| `{player}` | Name | `Steve` |
| `{ping}` | Ping in milliseconds | `42` |
| `{pingColor}` | A color code for the ping: green below 150, yellow below 300, red below 600, dark red below 1000, gray above | |
| `{dimension}` | Dimension the player is in | `Overworld` |
| `{x}` `{y}` `{z}` | Block position | `-43` |
| `{biome}` | Biome at the player's position | `Plains` |
| `{health}` | Health, rounded up | `20` |
| `{food}` | Food level | `18` |
| `{level}` | Experience level | `30` |
| `{gamemode}` | Game mode | `survival` |
| `{team}` | Team name, empty without a team | `Miners` |
| `{playtime}` | Time played on this server | `3h 5m` |
| `{deaths}` | Number of deaths | `4` |

## Proxies

A proxy can still set header and footer over the `SU|TabHF` plugin channel. The server uses the same channel for text
with variables, so do not use variables in `tab.headerText` and `tab.footerText` on a server behind a proxy that sends
its own header and footer: the two would take turns.
