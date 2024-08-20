# SkinRestorer

<a href="https://www.curseforge.com/minecraft/mc-mods/skinrestorer"><img src="https://raw.githubusercontent.com/Suiranoil/badges/main/assets/minecraft/platform/curseforge/mini/badge.svg" alt="CurseForge" height="32"></a>
<a href="https://modrinth.com/mod/skinrestorer"><img src="https://raw.githubusercontent.com/Suiranoil/badges/main/assets/minecraft/platform/modrinth/mini/badge.svg" alt="Modrinth" height="32"></a>

A server-side mod for managing and restoring player skins.

## ✨ Features

- **Set skins from Mojang Account**: Fetch and apply skins using a valid Minecraft username.
- **Set skins from Ely.by**: Fetch and apply skins using a valid [Ely.by](https://ely.by/) username.
- **Set skins from URL**: Fetch and apply skins from any image URL, supporting both classic (Steve) and slim (Alex) skin models.
- **Automatic skin fetching**: Automatically fetch skin from Mojang/Ely.by when a player joins the server running in offline/insecure mode ([configurable](https://github.com/Suiranoil/SkinRestorer/wiki/Config)).
- **Singleplayer support**: Apply skins individually for each world.
- **Permissions API support**

## 📜 Command Usage Guide

### Set Mojang skin

Fetch and apply skins using a valid Minecraft username.

```
/skin set mojang <username> [<targets>]
```

- **Parameters**
    - `<username>`: Minecraft username to fetch the skin from.
    - `[<targets>]`: (Optional, server operators only) Player(s) to apply the skin to.

### Set Ely.by skin

Fetch and apply skins using a valid [Ely.by](https://ely.by/) username.

```
/skin set ely.by <username> [<targets>]
```

- **Parameters**
    - `<username>`: Ely.by username to fetch the skin from.
    - `[<targets>]`: (Optional, server operators only) Player(s) to apply the skin to.

### Set Web skin

Fetch and apply skins from any image URL, supporting both classic (Steve) and slim (Alex) skin models.

Uses [mineskin api](https://mineskin.org/) under the hood.

```
/skin set web (classic|slim) "<url>" [<targets>]
```

- **Parameters**
    - `(classic|slim)`: Type of the skin model (`classic` for Steve model, `slim` for Alex model).
    - `"<url>"`: URL pointing to the skin image file (ensure it follows Minecraft's skin size and format requirements).
    - `[<targets>]`: (Optional, server operators only) Player(s) to apply the skin to.

### Refresh skin

Refetch and reapply the currently applied skins.

```
/skin refresh [<targets>]
```

- **Parameters**
    - `[<targets>]`: (Optional, server operators only) Player(s) to refresh the skin for.

### Clear skin

Remove the currently applied skins.

```
/skin clear [<targets>]
```

- **Parameters**
    - `[<targets>]`: (Optional, server operators only) Player(s) to clear the skin for.

### Reset skin

Reset to the default skin or remove any custom skin.

If [automatic skin fetching](https://github.com/Suiranoil/SkinRestorer/wiki/Config#fetchskinonfirstjoin) is enabled, a new skin will be fetched when the player rejoins the server.

```
/skin reset [<targets>]
```

- **Parameters**
    - `[<targets>]`: (Optional, server operators only) Player(s) to reset the skin for.

### Notes

- If `targets` is not specified, the command will apply to the player executing the command.

### Examples

```
/skin set mojang Notch
/skin set web classic "https://example.com/skin.png"
/skin clear @a
```
