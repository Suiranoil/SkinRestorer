package net.lionarius.skinrestorer.config.provider.custom;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.AbstractProviderConfig;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.util.Result;

public abstract class CustomProviderConfig extends AbstractProviderConfig {
    protected static final CacheConfig DEFAULT_CACHE_VALUE = new CacheConfig(true, 300);
    
    protected CustomProviderType type;
    protected boolean useProviderSignature = false;
    
    protected CustomProviderConfig(CustomProviderType type) {
        super(true, "", DEFAULT_CACHE_VALUE);
        this.type = type;
    }
    
    public CustomProviderType type() {
        return this.type;
    }
    
    public boolean useProviderSignature() {
        return this.useProviderSignature;
    }
    
    public abstract Result<SkinProvider, String> createSkinProvider(SkinSigner skinSigner);
    
    @Override
    public void gsonPostProcess() {
        this.validateCache(DEFAULT_CACHE_VALUE);
        
        if (this.type == null) {
            SkinRestorer.LOGGER.warn("Custom provider type is null, defaulting to UNKNOWN");
            this.type = CustomProviderType.UNKNOWN;
        }
        
        if (this.name == null) {
            SkinRestorer.LOGGER.warn("Custom provider name is null, defaulting to an empty string");
            this.name = "";
        } else {
            this.name = this.name.trim();
        }
    }
}
