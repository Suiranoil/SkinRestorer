package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

public abstract class BuiltInProviderConfig implements GsonPostProcessable {
    protected boolean enabled;
    protected String name;
    protected CacheConfig cache;
    
    
    public BuiltInProviderConfig(String name, CacheConfig cache) {
        this(name, cache, true);
    }
    
    public BuiltInProviderConfig(String name, CacheConfig cache, boolean enabled) {
        this.enabled = enabled;
        this.name = name;
        this.cache = cache;
    }
    
    public boolean enabled() {
        return enabled;
    }
    
    public String name() {
        return name;
    }
    
    public CacheConfig cache() {
        return cache;
    }
    
    protected void validate(String defaultName, CacheConfig defaultCache) {
        if (this.name == null || this.name.isEmpty()) {
            SkinRestorer.LOGGER.warn("Provider name is null or empty, defaulting to '{}'", defaultName);
            this.name = defaultName;
        }
        
        if (this.cache == null) {
            SkinRestorer.LOGGER.warn("Provider cache is null, using default");
            this.cache = defaultCache;
        } else {
            this.cache.validate(defaultCache);
        }
    }
}
