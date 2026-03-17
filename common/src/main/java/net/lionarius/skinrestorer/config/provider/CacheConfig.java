package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.SkinRestorer;

public final class CacheConfig {
    private boolean enabled;
    private long duration;

    public CacheConfig(boolean enabled, long duration) {
        this.enabled = enabled;
        this.duration = duration;
    }

    public boolean enabled() {
        return enabled;
    }

    public long duration() {
        return duration;
    }

    void validate(CacheConfig defaultValue) {
        if (this.duration <= 0) {
            SkinRestorer.LOGGER.warn(
                    "Cache duration is less than or equal to zero, defaulting to {}", defaultValue.duration());
            this.duration = defaultValue.duration();
        }
    }
}
