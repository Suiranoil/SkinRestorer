package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.ElyBySkinProvider;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

public class ElyByProviderConfig implements BuiltInProviderConfig, GsonPostProcessable {
    private static final CacheConfig DEFAULT_CACHE_VALUE = new CacheConfig(true, 60);
    
    private boolean enabled;
    private String name;
    private CacheConfig cache;
    
    public ElyByProviderConfig() {
        this.enabled = true;
        this.name = ElyBySkinProvider.PROVIDER_NAME;
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
            SkinRestorer.LOGGER.warn("Ely.By provider name is null or empty, defaulting to '{}'", ElyBySkinProvider.PROVIDER_NAME);
            this.name = ElyBySkinProvider.PROVIDER_NAME;
        }
        
        if (this.cache == null) {
            SkinRestorer.LOGGER.warn("Ely.By provider cache is null, using default");
            this.cache = DEFAULT_CACHE_VALUE;
        } else {
            this.cache.validate(DEFAULT_CACHE_VALUE);
        }
    }
}
