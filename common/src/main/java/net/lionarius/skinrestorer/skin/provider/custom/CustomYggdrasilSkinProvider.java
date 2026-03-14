package net.lionarius.skinrestorer.skin.provider.custom;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomYggdrasilProviderConfig;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinResigner;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.skin.provider.base.YggdrasilSkinProvider;
import net.lionarius.skinrestorer.util.WebUtils;

import java.net.URI;
import java.util.Optional;

public final class CustomYggdrasilSkinProvider extends YggdrasilSkinProvider {
    private final String providerName;
    private final SkinResigner skinResigner;
    
    private CacheConfig cacheConfig;
    private URI baseServicesServerUrl;
    private URI baseSessionServerUrl;
    
    public CustomYggdrasilSkinProvider(String providerName, SkinSigner skinSigner) {
        this.providerName = providerName;
        this.skinResigner = new SkinResigner(skinSigner);
    }
    
    @Override
    public String getProviderName() {
        return this.providerName;
    }
    
    @Override
    public void reload() {
        var config = SkinRestorer.getConfig()
                .providers()
                .findCustomByName(this.providerName, CustomYggdrasilProviderConfig.class)
                .orElse(null);
        
        if (config == null) {
            SkinRestorer.LOGGER.warn("Could not find config for custom provider '{}'", this.providerName);
            this.baseServicesServerUrl = null;
            this.baseSessionServerUrl = null;
            this.cacheConfig = null;
            return;
        }
        
        this.skinResigner.reload(config.useProviderSignature());
        this.reloadUrls(config);
        this.cacheConfig = config.cache();
        this.createSkinCache();
    }
    
    private void reloadUrls(CustomYggdrasilProviderConfig config) {
        this.baseServicesServerUrl = WebUtils.parseUri(config.servicesUrl());
        this.baseSessionServerUrl = WebUtils.parseUri(config.sessionUrl());
    }
    
    @Override
    protected CacheConfig getCacheConfig() {
        return this.cacheConfig;
    }
    
    @Override
    protected void validate(String argument, SkinVariant variant) throws Exception {
        super.validate(argument, variant);
        if (this.baseServicesServerUrl == null || this.baseSessionServerUrl == null)
            throw new IllegalStateException("Custom provider '" + this.providerName + "' has invalid URLs");
    }
    
    @Override
    protected Optional<Property> extractSkin(GameProfile profile) throws Exception {
        return this.skinResigner.extractSkin(profile);
    }
    
    @Override
    protected URI baseSessionServerUrl() {
        return this.baseSessionServerUrl;
    }
    
    @Override
    protected URI baseServicesServerUrl() {
        return this.baseServicesServerUrl;
    }
}
