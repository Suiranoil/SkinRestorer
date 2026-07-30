# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.10.0] - 2026-07-31
### Added
- Added an optional `name` field to collection provider sources, allowing players to pick a specific skin with `/skin set collection <name>` (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#providerscollection)) ([#113](https://github.com/Suiranoil/SkinRestorer/issues/113))
- Added tab-completion for skin provider command arguments: online players for username-based providers, named skins for the collection provider
- Added the `providers.mineskin.allowedDomains` config option to restrict which domains players can use with `/skin set web` (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#providersmineskin))
- Added the `command` config section to control who can change skins: `command.enabled`, `command.permissionLevel` and `command.targetsPermissionLevel` (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#command)) ([#114](https://github.com/Suiranoil/SkinRestorer/issues/114))
### Changed
- `/skin set web` now only accepts `http(s)` URLs
- `/skin config reload` now resends the command tree to online players, so command changes apply without relogging

## [2.9.0] - 2026-07-07
### Added
- Added the `storage.location` config option to store skins globally (shared across all worlds) instead of per world ([#110](https://github.com/Suiranoil/SkinRestorer/issues/110))

## [2.8.1] - 2026-06-10
### Changed
- Improved logging of unexpected errors during skin fetch on login
### Fixed
- Fixed skin changes being lost if the server crashed before the player disconnected (skins are now saved to disk immediately)
- Fixed usernames with special characters not being properly encoded in skin provider requests
- Fixed the custom authlib injector provider blocking the server thread while resolving its API root
- Fixed the MineSkin HTTP client and its worker threads not being closed when reloading the config
- Fixed skins of players that disconnect during login staying in the memory cache
- Fixed `request.userAgent` changes sometimes not applying after a config reload
- Fixed a rare issue where delayed skin apply tasks of different players could conflict
- Fixed an error being logged on first startup when no config file exists yet

## [2.8.0] - 2026-06-01
### Added
- Added Japanese translation ([#100](https://github.com/Suiranoil/SkinRestorer/pull/100)) - *undefined9651*
- Added fallback providers support for automatic skin fetch ([#102](https://github.com/Suiranoil/SkinRestorer/pull/102)) - *Carto1a*
### Changed
- Renamed `autoFetch.provider` to `autoFetch.providers` (now a list, automatically migrated from old format)
- Improved German, Hindi, Hungarian, Ukrainian, and Simplified Chinese translations
- Improved performance and stability by running skin fetches on a dedicated thread pool
### Fixed
- Fixed player login hanging when a skin provider accepts the connection but never responds
- Fixed saved skins being lost if the server crashed while saving (skin files are now written atomically)
- Fixed a possible crash when using the collection provider with certain seeds
- Fixed provider skin caches growing without an upper bound
- Fixed the HTTP client not being closed when reloading the config

## [2.7.1] - 2026-03-30
### Added
- Added support for minecraft 26.1

## [2.7.0] - 2026-03-19
### Added
- Added versioning and migration system for config and skin data files
- Added `join.skipRefreshProviders` config option to skip skin refresh on join for specific providers (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#joinskiprefreshproviders))
- Added `request.userAgent` config option for customizing the HTTP user agent (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#requestuseragent))
### Changed
- Reorganized config structure into `join`, `request`, and `autoFetch` sections (automatically migrated from old format)
### Fixed
- Fixed bundled player info remove and update packets causing skin update issues (fixes [#96](https://github.com/Suiranoil/SkinRestorer/issues/96))

## [2.6.0] - 2026-02-25
### Added
- Added custom skin providers system for configuring third-party skin sources (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#providerscustom))
    - Custom Yggdrasil provider (for custom authentication servers)
    - Custom Authlib Injector provider
    - Custom Username URL provider (fetch skins by URL using player username)
- Added `proxyUrlUpload` option for MineSkin provider (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#providersmineskin))
### Fixed
- Fixed ely.by provider using HTTP URL instead of HTTPS
- Fixed `/skin set` with multiple targets sometimes saving the wrong original skin for reset

## [2.5.0] - 2026-01-14
### Added
- Added collection skin provider (allows assigning random skins from a predefined set)
- Added `forceFirstJoinSkinFetch` config option to force skin fetch on first join even if player already has a skin

## [2.4.3] - 2025-07-25
### Fixed
- Fixed crash on client when loading player head skin (fixes [#63](https://github.com/Suiranoil/SkinRestorer/issues/63) and [#64](https://github.com/Suiranoil/SkinRestorer/issues/64))
- Fixed server freeze when loading player head skin

## [2.4.2] - 2025-07-13
### Fixed
- Fix crash when head profile name is null (fixes [#60](https://github.com/Suiranoil/SkinRestorer/issues/60) and [#61](https://github.com/Suiranoil/SkinRestorer/issues/61))

## [2.4.1] - 2025-07-09
### Changed
- Log full exception and argument when unable to fetch/set skin
### Fixed
- Fixed mojang provider using offline uuids when unable to fetch actual uuid resulting in `no profile with uuid` error

## [2.4.0] - 2025-07-05
### Fixed
- Added support for player heads

## [2.3.5] - 2025-06-21
### Fixed
- Fix mod not loading on client

## [2.3.4] - 2025-06-19
### Added
- Added support for minecraft 1.21.6

## [2.3.3] - 2025-06-01
### Fixed
- Fixed forge mixin crash (closes [#54](https://github.com/Suiranoil/SkinRestorer/issues/53))
### Removed
- Removed minecraft 1.19 support

## [2.3.2] - 2025-05-24
### Fixed
- Fixed mixin incompatibility with ModernFix (closes [#42](https://github.com/Suiranoil/SkinRestorer/issues/52))

## [2.3.1] - 2025-05-03
### Added
- Added support for minecraft 1.19-1.19.4
### Changed
- Use services and session server urls from environment

## [2.3.0] - 2025-03-27
### Added
- Added `skinApplyDelayOnJoin` config option (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#skinapplydelayonjoin))
### Changed
- Updated to 1.21.5
### Removed
- [NeoForge] Dropped support for NeoForge on Minecraft 1.20.5-1.20.6

## [2.2.1] - 2024-12-23
### Fixed
- Fixed game not closing because of mineskin working threads (closes [#41](https://github.com/Suiranoil/SkinRestorer/issues/41))

## [2.2.0] - 2024-12-02
### Added
- Added support for [SkinShuffle](https://modrinth.com/mod/skinshuffle) clients (requires FabricAPI on Fabric)
  (closes [#34](https://github.com/Suiranoil/SkinRestorer/issues/34))
- Added `providers.mineskin.apiKey` config option (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#providersmineskin))
### Changed
- Migrated to MineSkin's new API V2
### Fixed
- Fixed `providers` config validation
### Removed
- Dropped support for NeoForge on Minecraft 1.20.2-1.20.4

## [2.1.0] - 2024-09-26
### Added
- Added `config reload` command for dynamic configuration updates
- Added `refreshSkinOnJoin` config option (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#refreshskinonjoin))
- Implemented caching for skin providers
- Added provider configurations (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#providers))
- Added username and url validation for requests
### Fixed
- Fixed old skin directory migration not working
- Prevented overwriting existing skin files during migration

## [2.0.3] - 2024-08-20
### Added
- Added `firstJoinSkinProvider` config option (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Config#firstjoinskinprovider))
### Fixed
- Fixed ability to set config values to `null`

## [2.0.2] - 2024-08-04
### Added
- Backported to minecraft 1.20-1.20.2
- [Forge] Added support for Forge
### Changed
- Changed minimum java version to 17 for minecraft 1.20-1.20.4

## [2.0.1] - 2024-07-16
### Added
- Added Traditional Chinese translation ([#24](https://github.com/Suiranoil/SkinRestorer/pull/24)) - *yichifauzi*, *notlin4*
### Fixed
- Fixed concurrent modification exception

## [2.0.0] - 2024-07-03
### Added
- Added support for NeoForge
- Added support for singleplayer
- Added ely.by skin provider
- Added `/skin refresh` command to get up-to-date skin
- Added `/skin reset` command to remove skin data
- Added proper support for secure servers
- Added support for permissions api
- Added translations
### Changed
- Changed to save skin data per world
### Fixed
- Fixed entity flickering on skin reload
- Fixed player being left in invalid state on skin reload if on death screen

## [1.2.6] - 2024-06-25
### Added
- Added support for command blocks
### Changed
- Changed to better skin refresh logic
### Fixed
- Fixed no permission level for /skin clear targets
- Fixed loading world screen on skin change

## [1.2.4] - 2023-09-30
### Fixed
- Fixed player stops riding entity with skin change
- Fixed armor and hand items not displaying with skin change

## [1.2.3] - 2023-06-10
### Changed
- Updated to 1.20-1.20.1

## [1.2.2] - 2022-12-21
### Changed
- Updated to 1.19.3

## [1.2.1] - 2022-11-24
### Fixed
- Fixed incorrect filepath resolvement on linux systems

## [1.2.0] - 2022-11-18
### Added
- Added support for fake players ([#4](https://github.com/Suiranoil/SkinRestorer/pull/4)) - *CaveNightingale*
### Changed
- Allow player to apply their skin changes immediately ([#4](https://github.com/Suiranoil/SkinRestorer/pull/4)) - _CaveNightingale_

## [1.0.4] - 2021-08-28
### Fixed
- Fixed "invalid player data" exception
