### Added
- Added custom skin providers system for configuring third-party skin sources (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#providerscustom))
    - Custom Yggdrasil provider (for custom authentication servers)
    - Custom Authlib Injector provider
    - Custom Username URL provider (fetch skins by URL using player username)
- Added `proxyUrlUpload` option for MineSkin provider (see [wiki](https://github.com/Suiranoil/SkinRestorer/wiki/Configuration#providersmineskin))
### Fixed
- Fixed ely.by provider using HTTP URL instead of HTTPS
- Fixed `/skin set` with multiple targets sometimes saving the wrong original skin for reset
