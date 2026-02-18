package net.lionarius.skinrestorer.skin.provider.custom;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomYggdrasilProviderConfig;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.skin.provider.YggdrasilSkinProvider;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.WebUtils;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class CustomYggdrasilSkinProvider extends YggdrasilSkinProvider {
    private final String providerName;
    private final SkinSigner skinSigner;

    private LoadingCache<String, Optional<Property>> skinCache;
    private Cache<Integer, Property> signatureCache;
    
    private boolean useProviderSignature;
    private URI baseServicesServerUrl;
    private URI baseSessionServerUrl;

    public CustomYggdrasilSkinProvider(String providerName, SkinSigner skinSigner) {
        this.providerName = providerName;
        this.skinSigner = skinSigner;
        this.useProviderSignature = false;
    }

    @Override
    public String getProviderName() {
        return this.providerName;
    }

    @Override
    public void reload() {
        var config = SkinRestorer.getConfig()
                .providersConfig()
                .findCustomByName(this.providerName, CustomYggdrasilProviderConfig.class)
                .orElse(null);

        if (config == null) {
            SkinRestorer.LOGGER.warn("Could not find config for custom provider '{}'", this.providerName);
            this.baseServicesServerUrl = null;
            this.baseSessionServerUrl = null;
            this.skinCache = null;
            this.signatureCache = null;
            return;
        }

        this.useProviderSignature = config.useProviderSignature();
        this.reloadUrls(config);
        this.createCache(config.cache());
    }

    private void reloadUrls(CustomYggdrasilProviderConfig config) {
        this.baseServicesServerUrl = WebUtils.parseUri(config.servicesUrl());
        this.baseSessionServerUrl = WebUtils.parseUri(config.sessionUrl());
    }

    private void createCache(CacheConfig config) {
        var time = config.enabled() ? config.duration() : 0;

        this.skinCache = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Optional<Property> load(@NotNull String key) throws Exception {
                        return CustomYggdrasilSkinProvider.this.loadSkin(key);
                    }
                });

        if (this.useProviderSignature) {
            this.signatureCache = null;
        } else {
            this.signatureCache = CacheBuilder.newBuilder()
                    .expireAfterAccess(24, TimeUnit.HOURS)
                    .maximumSize(1000)
                    .build();
        }
    }

    @Override
    protected Optional<Property> fetchSkinImpl(String username) throws Exception {
        if (this.skinCache == null)
            throw new IllegalStateException("Custom provider '" + this.providerName + "' is not initialized");

        if (this.baseServicesServerUrl == null || this.baseSessionServerUrl == null)
            throw new IllegalStateException("Custom provider '" + this.providerName + "' has invalid URLs");

        return this.skinCache.get(username.toLowerCase(Locale.ROOT));
    }

    private Optional<Property> loadSkin(String username) throws Exception {
        var profileId = this.getProfile(username).id();
        var profile = this.getProfileWithProperties(profileId);
        
        if (this.useProviderSignature) {
            var textures = PlayerUtils.getPlayerSkin(profile);
            return Optional.ofNullable(textures);
        }

        var skin = PlayerUtils.getPlayerSkin(profile);
        if (skin == null)
            return Optional.empty();

        if (PlayerUtils.getSkinUrl(skin) == null)
            return Optional.empty();

        var propertyHash = skin.value().hashCode();
        var cachedSignature = this.signatureCache == null ? null : this.signatureCache.getIfPresent(propertyHash);
        if (cachedSignature != null)
            return Optional.of(cachedSignature);
        
        var signed = this.skinSigner.signSkin(skin);
        signed.ifPresent(property -> {
            if (this.signatureCache != null)
                this.signatureCache.put(propertyHash, property);
        });
        
        return signed;
    }

    @Override
    protected URI baseSessionServerUrl() {
        if (this.baseSessionServerUrl == null)
            throw new IllegalStateException("Missing session URL for provider '" + this.providerName + "'");

        return this.baseSessionServerUrl;
    }

    @Override
    protected URI baseServicesServerUrl() {
        if (this.baseServicesServerUrl == null)
            throw new IllegalStateException("Missing services URL for provider '" + this.providerName + "'");

        return this.baseServicesServerUrl;
    }
}
