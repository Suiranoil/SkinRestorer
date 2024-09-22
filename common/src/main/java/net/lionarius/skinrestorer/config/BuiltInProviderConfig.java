package net.lionarius.skinrestorer.config;

public record BuiltInProviderConfig(boolean enabled, String name, CacheConfig cache) {
    public boolean isValid() {
        return this.name != null && this.cache != null && !this.name.isEmpty() && this.cache.isValid();
    }
}
