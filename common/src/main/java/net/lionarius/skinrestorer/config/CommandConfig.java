package net.lionarius.skinrestorer.config;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

public final class CommandConfig implements GsonPostProcessable {
    public static final int MIN_PERMISSION_LEVEL = 0;
    public static final int MAX_PERMISSION_LEVEL = 4;

    private boolean enabled = true;

    private int permissionLevel = 0;

    private int targetsPermissionLevel = 2;

    public boolean enabled() {
        return this.enabled;
    }

    public int permissionLevel() {
        return this.permissionLevel;
    }

    public int targetsPermissionLevel() {
        return this.targetsPermissionLevel;
    }

    @Override
    public void gsonPostProcess() {
        this.permissionLevel = CommandConfig.clampPermissionLevel(this.permissionLevel, "PermissionLevel");
        this.targetsPermissionLevel =
                CommandConfig.clampPermissionLevel(this.targetsPermissionLevel, "TargetsPermissionLevel");
    }

    private static int clampPermissionLevel(int level, String name) {
        if (level >= MIN_PERMISSION_LEVEL && level <= MAX_PERMISSION_LEVEL) return level;

        var clamped = Math.min(Math.max(level, MIN_PERMISSION_LEVEL), MAX_PERMISSION_LEVEL);
        SkinRestorer.LOGGER.warn(
                "{} config is outside of the [{}, {}] range, defaulting to {}",
                name,
                MIN_PERMISSION_LEVEL,
                MAX_PERMISSION_LEVEL,
                clamped);

        return clamped;
    }
}
