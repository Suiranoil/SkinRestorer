package net.lionarius.skinrestorer.config;

public record CacheConfig(boolean enabled, long duration) {
    public boolean isValid() {
        return this.duration > 0;
    }
}
