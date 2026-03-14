package net.lionarius.skinrestorer.skin.provider.custom;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomUsernameUrlProviderConfig;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.skin.provider.base.UsernameSkinProvider;

import java.net.URI;
import java.util.Optional;

public final class CustomUsernameUrlSkinProvider extends UsernameSkinProvider {
    public static final String USERNAME_PLACEHOLDER = "{username}";
    
    private static final String VALIDATION_USERNAME = "example";
    
    private final String providerName;
    private final SkinSigner skinSigner;
    
    private CacheConfig cacheConfig;
    private String urlTemplate;
    
    public CustomUsernameUrlSkinProvider(String providerName, SkinSigner skinSigner) {
        this.providerName = providerName;
        this.skinSigner = skinSigner;
    }
    
    @Override
    public String getProviderName() {
        return this.providerName;
    }
    
    @Override
    public void reload() {
        var config = SkinRestorer.getConfig()
                .providers()
                .findCustomByName(this.providerName, CustomUsernameUrlProviderConfig.class)
                .orElse(null);
        
        if (config == null) {
            SkinRestorer.LOGGER.warn("Could not find config for custom provider '{}'", this.providerName);
            this.cacheConfig = null;
            this.urlTemplate = null;
            return;
        }
        
        this.cacheConfig = config.cache();
        this.urlTemplate = this.resolveUrlTemplate(config.urlTemplate());
        this.createSkinCache();
    }
    
    private String resolveUrlTemplate(String template) {
        if (CustomUsernameUrlSkinProvider.isValidUrlTemplate(template))
            return template;
        
        SkinRestorer.LOGGER.warn("Custom provider '{}' has invalid urlTemplate '{}'", this.providerName, template);
        return null;
    }
    
    public static boolean isValidUrlTemplate(String urlTemplate) {
        if (urlTemplate == null || urlTemplate.isEmpty())
            return false;
        
        if (!urlTemplate.contains(CustomUsernameUrlSkinProvider.USERNAME_PLACEHOLDER))
            return false;
        
        try {
            URI uri = new URI(urlTemplate.replace(
                    CustomUsernameUrlSkinProvider.USERNAME_PLACEHOLDER,
                    CustomUsernameUrlSkinProvider.VALIDATION_USERNAME
            ));
            return uri.isAbsolute();
        } catch (Exception ignored) {
            return false;
        }
    }
    
    @Override
    protected CacheConfig getCacheConfig() {
        return this.cacheConfig;
    }
    
    @Override
    protected void validate(String argument, SkinVariant variant) throws Exception {
        super.validate(argument, variant);
        if (this.urlTemplate == null)
            throw new IllegalStateException("Custom provider '" + this.providerName + "' has invalid urlTemplate");
    }
    
    @Override
    protected String getCacheKey(String argument, SkinVariant variant) {
        return argument;
    }
    
    @Override
    protected Optional<Property> loadSkin(String username) throws Exception {
        var url = this.urlTemplate.replace(CustomUsernameUrlSkinProvider.USERNAME_PLACEHOLDER, username);
        return this.skinSigner.signSkin(new URI(url), null);
    }
}
