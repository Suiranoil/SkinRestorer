### Added
- Added an optional `name` field to collection provider sources, allowing players to pick a specific skin with `/skin set collection <name>` (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#providerscollection)) ([#113](https://github.com/Suiranoil/SkinRestorer/issues/113))
- Added tab-completion for skin provider command arguments: online players for username-based providers, named skins for the collection provider
- Added the `providers.mineskin.allowedDomains` config option to restrict which domains players can use with `/skin set web` (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#providersmineskin))
- Added the `command` config section to control who can change skins: `command.enabled`, `command.permissionLevel` and `command.targetsPermissionLevel` (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#command)) ([#114](https://github.com/Suiranoil/SkinRestorer/issues/114))
### Changed
- `/skin set web` now only accepts `http(s)` URLs
- `/skin config reload` now resends the command tree to online players, so command changes apply without relogging
