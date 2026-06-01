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
