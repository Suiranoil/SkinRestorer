package net.lionarius.skinrestorer.config.provider;

public abstract class BuiltInProviderConfig extends AbstractProviderConfig {
    
    
    public BuiltInProviderConfig(String name, CacheConfig cache) {
        this(name, cache, true);
    }
    
    public BuiltInProviderConfig(String name, CacheConfig cache, boolean enabled) {
        super(enabled, name, cache);
    }
    
    protected void validate(String defaultName, CacheConfig defaultCache) {
        this.validateName(defaultName);
        this.validateCache(defaultCache);
    }
}
