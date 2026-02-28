package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

public abstract class AbstractProviderConfig implements GsonPostProcessable {
    protected boolean enabled;
    protected String name;
    protected CacheConfig cache;
    
    protected AbstractProviderConfig(boolean enabled, String name, CacheConfig cache) {
        this.enabled = enabled;
        this.name = name;
        this.cache = cache;
    }
    
    public boolean enabled() {
        return this.enabled;
    }
    
    public String name() {
        return this.name;
    }
    
    public CacheConfig cache() {
        return this.cache;
    }
    
    protected void validateName(String defaultName) {
        if (this.name == null || this.name.isBlank()) {
            SkinRestorer.LOGGER.warn("Provider name is null or empty, defaulting to '{}'", defaultName);
            this.name = defaultName;
            return;
        }
        
        this.name = this.name.trim();
    }
    
    protected void validateCache(CacheConfig defaultCache) {
        if (this.cache == null) {
            SkinRestorer.LOGGER.warn("Provider cache is null, using default");
            this.cache = defaultCache;
        } else {
            this.cache.validate(defaultCache);
        }
    }
}
