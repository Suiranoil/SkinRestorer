package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.skin.provider.builtin.MojangSkinProvider;

public final class MojangProviderConfig extends BuiltInProviderConfig {
    private static final CacheConfig DEFAULT_CACHE_VALUE = new CacheConfig(true, 60);
    
    public MojangProviderConfig() {
        super(MojangSkinProvider.PROVIDER_NAME, DEFAULT_CACHE_VALUE);
    }
    
    @Override
    public void gsonPostProcess() {
        super.validate(MojangSkinProvider.PROVIDER_NAME, DEFAULT_CACHE_VALUE);
    }
}
