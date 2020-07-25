# BackupServer PaperMC Plugin

A plugin for Paper MineCraft servers that allows downloading backups on demand.

## What does this plugin do?

This plugin runs a lightweight embedded HTTP server within the MineCraft server. It exposes two endpoints:
- `GET /` renders a super-basic HTML page with a link to `/backup`.
- `GET /backup` returns a `.tar.gz` file containing a backup of the server.

Backups are created by accessing the `GET /backup` endpoint. Backups are only returned over HTTP - they are never stored
on disk or uploaded anywhere.

## Why use this plugin?

The big difference between this plugin and other backup plugins like [eBackup](https://github.com/espidev/ebackup) is
that backups are never stored on disk. While this does mean the backup itself takes longer to generate, it also means
precious disk isn't taken up on the server. If you're running on your own server hardware, this probably isn't an issue.
But if you're running a hosted server, disk space is likely at a premium and backups take up a significant portion of
the available space.

## Getting Started

1. Get the `.jar` file from the [latest release](https://github.com/kwvanderlinde/BackupServerMCPlugin/releases/latest)
   on GitHub.
2. Drop the `.jar` file into your server's `plugins/` directory.
3. (Re)Start your server.
4. Edit `plugins/BackupServer/config.yml` so that the settings match your server configurations.
   - You must change `http-server-port` to a port that is open on your server.
   - You may want to change `http-server-host` so that the log messages generate the correct URL. This is otherwise not
     needed.
   - You may want to change `basename-template` so that backup archives are automatically given a name that you like.
     This can always be changed on a case-by-case basic during file download. Valid substitutions are:
     - `${datetime}` for a human-readable time of the start of the backup, in UTC.
     - `${unixtime}` for the unix time of the start of the backup.
5. Restart your server.
6. Open up `http://{host}:{port}/backup`. If all is configured correclty, this should open a file download dialog and
   then start the backup.