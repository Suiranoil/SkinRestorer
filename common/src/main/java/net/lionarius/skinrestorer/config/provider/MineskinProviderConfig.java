package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.MineskinSkinProvider;

public final class MineskinProviderConfig extends BuiltInProviderConfig {
    private static final CacheConfig DEFAULT_CACHE_VALUE = new CacheConfig(true, 300);
    
    private String apiKey;
    
    public MineskinProviderConfig() {
        super(MineskinSkinProvider.PROVIDER_NAME, DEFAULT_CACHE_VALUE);
        
        this.apiKey = "";
    }
    
    public String apiKey() {
        return apiKey;
    }
    
    @Override
    public void gsonPostProcess() {
        super.validate(MineskinSkinProvider.PROVIDER_NAME, DEFAULT_CACHE_VALUE);
        
        if (this.apiKey == null) {
            SkinRestorer.LOGGER.warn("Mineskin API key is null, defaulting to an empty string");
            this.apiKey = "";
        }
    }
}
