### Added
- Added versioning and migration system for config and skin data files
- Added `join.skipRefreshProviders` config option to skip skin refresh on join for specific providers (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#joinskiprefreshproviders))
- Added `request.userAgent` config option for customizing the HTTP user agent (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#requestuseragent))
### Changed
- Reorganized config structure into `join`, `request`, and `autoFetch` sections (automatically migrated from old format)
### Fixed
- Fixed bundled player info remove and update packets causing skin update issues (fixes [#96](https://github.com/Suiranoil/SkinRestorer/issues/96))
