package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.skin.provider.DraslSkinProvider;

public final class DraslProviderConfig extends BuiltInProviderConfig {
    private static final CacheConfig DEFAULT_CACHE_VALUE = new CacheConfig(true, 60);
    
    private String url;

    public DraslProviderConfig() {
        super(DraslSkinProvider.PROVIDER_NAME, DEFAULT_CACHE_VALUE);

        this.url = "";
    }
    
    public String url() {
        return url;
    }

    @Override
    public void gsonPostProcess() {
        super.validate(DraslSkinProvider.PROVIDER_NAME, DEFAULT_CACHE_VALUE);

        if (this.url == null) {
            SkinRestorer.LOGGER.warn("Drasl base URL not set");
            this.url = "";
        }
    }
}
