# AvenoxLeaderboard 2.2.0

## Profile/stat improvements
- Clicking any player entry in a leaderboard opens that player's full stats GUI.
- `/stats` opens your own stats.
- `/stats <player>` opens the selected player's stats.
- Online targets use live Bukkit statistics where available; offline targets use persistent leaderboard data.
- First Joined is always taken from the selected target, not the viewer.
- Leaderboard entries use individual clean hex (`&#RRGGBB`) accents instead of a single red color.
- Entry lore is professional and uses `%color%` so each leaderboard keeps its own accent.
- Players are still seeded at `0` on join on every enabled leaderboard.

## Build
Java release: 25.

Use Maven with a JDK 25 installation: `mvn clean package`.
# AvenoxLeaderboard

AvenoxLeaderboard is a Java 25 leaderboard plugin designed for Paper/Folia-compatible servers.

## Current GUI updates

- Professional red/white GUI styling using `&#ff0000` as the primary accent.
- Cleaner leaderboard category and entry lore.
- The leaderboard bottom-right button is now **YOUR STATS**.
- Clicking **YOUR STATS** opens the player's personal statistics GUI directly.
- Personal statistics GUI follows the supplied 27-slot reference layout.
- Dynamic stats title: `<player>'s Stats`.
- Added **PROFILE** information with the player's first-joined date.
- Added a dedicated **FIRST JOINED** item.
- First-joined timestamps use the server's local timezone and `MM/dd/yy HH:mm:ss` format.

## Player tracking update

Every joining player is seeded into every enabled leaderboard with a value of `0` (subject to an existing world/permission filter and the `universalleaderboards.exclude` permission). This means a new player does not need to perform an action first just to receive a leaderboard entry.

The normal asynchronous refresh then replaces that zero seed with the real value for statistic, money, counter, or PlaceholderAPI-backed boards.

`max-entries` still controls how many players are displayed on a leaderboard page. If a server has more tracked players than the configured maximum, only the configured top N entries can be displayed.

## Build

The project is configured for Java 25 in `pom.xml`. Build with a JDK 25 Maven installation:

```text
mvn clean package
```
