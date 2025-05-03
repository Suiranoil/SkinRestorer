# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
