# Darts Battle 2.4.0

Server 2.4.0 must be deployed before the new client. Existing clients remain able
to play a single match; both PvP players need the new client for rematches and a
call to continue across rounds. Bot games support four difficulties and retain
the selected difficulty on replay.

The database upgrade is additive and runs when the server starts. Before
starting the upgraded server, back up the MySQL database using mysqldump and
test the upgrade twice on a restored copy. New match records include session,
round, final scores and bot difficulty; new throws record whether they were
BUST. Existing rows are not rewritten. The detail screen marks reconstructed
or incomplete old data. Restoring a backup is an administrative recovery action
and must account for games played after that backup.

The leaderboard and public player pages expose only completed PvP matches.
Players can also see their own bot matches in their personal history. TURN
configuration and credentials remain server-side; this release requires no
change to the tunnel or TURN settings.

The Windows x64 client ZIP contains its own runtime. Extract the entire ZIP and
open DartsBattle.exe. Distribute the ZIP to both players: prior releases do not
include automatic updates. Successful local/native tests and issued TURN
credentials do not establish that media works between two external networks;
verify a real two-network call and rematch before declaring that path tested.
