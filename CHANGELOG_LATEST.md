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
