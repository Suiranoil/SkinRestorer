package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.MojangSkinProvider;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

public class MojangProviderConfig implements BuiltInProviderConfig, GsonPostProcessable {
    private static final CacheConfig DEFAULT_CACHE_VALUE = new CacheConfig(true, 60);
    
    private boolean enabled;
    private String name;
    private CacheConfig cache;
    
    public MojangProviderConfig() {
        this.enabled = true;
        this.name = MojangSkinProvider.PROVIDER_NAME;
        this.cache = DEFAULT_CACHE_VALUE;
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
    
    @Override
    public void gsonPostProcess() {
        if (this.name == null || this.name.isEmpty()) {
            SkinRestorer.LOGGER.warn("Mojang provider name is null or empty, defaulting to '{}'", MojangSkinProvider.PROVIDER_NAME);
            this.name = MojangSkinProvider.PROVIDER_NAME;
        }
        
        if (this.cache == null) {
            SkinRestorer.LOGGER.warn("Mojang provider cache is null, using default");
            this.cache = DEFAULT_CACHE_VALUE;
        } else {
            this.cache.validate(DEFAULT_CACHE_VALUE);
        }
    }
}
