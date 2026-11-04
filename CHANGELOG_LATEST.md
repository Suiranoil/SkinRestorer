### Fixed
- Fixed skins applied with SkinShuffle being refreshed on join, which always failed and logged a warning
- Fixed `/skin refresh` failing for skins applied with SkinShuffle
- Fixed other mods looking up a player without a saved skin making that player count as having one (e.g. skipping the first join skin fetch)
- Fixed a rare error when a player's skin was looked up while they were disconnecting
- Fixed the broken Forge jars for 1.19.1–1.19.2 (crash on startup) and 1.19.3 (missing mod code)
