package net.lionarius.skinrestorer.config.provider;

import net.lionarius.skinrestorer.skin.provider.builtin.ElyBySkinProvider;

public final class ElyByProviderConfig extends BuiltInProviderConfig {
    private static final CacheConfig DEFAULT_CACHE_VALUE = new CacheConfig(true, 60);

    public ElyByProviderConfig() {
        super(ElyBySkinProvider.PROVIDER_NAME, DEFAULT_CACHE_VALUE);
    }

    @Override
    public void gsonPostProcess() {
        super.validate(ElyBySkinProvider.PROVIDER_NAME, DEFAULT_CACHE_VALUE);
    }
}
