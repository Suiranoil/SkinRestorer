package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.MineskinSkinProvider;
import net.lionarius.skinrestorer.util.gson.GsonPostProcessable;

public class MineskinProviderConfig implements BuiltInProviderConfig, GsonPostProcessable {
    private static final CacheConfig DEFAULT_CACHE_VALUE = new CacheConfig(true, 300);
    
    private boolean enabled;
    private String name;
    private CacheConfig cache;
    
    public MineskinProviderConfig() {
        this.enabled = true;
        this.name = MineskinSkinProvider.PROVIDER_NAME;
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
            SkinRestorer.LOGGER.warn("Mineskin provider name is null or empty, defaulting to '{}'", MineskinSkinProvider.PROVIDER_NAME);
            this.name = MineskinSkinProvider.PROVIDER_NAME;
        }
        
        if (this.cache == null) {
            SkinRestorer.LOGGER.warn("Mineskin cache is null, using default");
            this.cache = DEFAULT_CACHE_VALUE;
        } else {
            this.cache.validate(DEFAULT_CACHE_VALUE);
        }
    }
}
