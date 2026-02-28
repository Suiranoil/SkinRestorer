package net.lionarius.skinrestorer.skin.provider.custom;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.util.UndashedUuid;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomAuthlibInjectorProviderConfig;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinResigner;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.skin.provider.base.ProfileSkinProvider;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.WebUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.UUID;

public final class CustomAuthlibInjectorSkinProvider extends ProfileSkinProvider {
    private final String providerName;
    private final SkinResigner skinResigner;
    
    private CacheConfig cacheConfig;
    private URI resolvedApiRoot;
    
    public CustomAuthlibInjectorSkinProvider(String providerName, SkinSigner skinSigner) {
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
                .providersConfig()
                .findCustomByName(this.providerName, CustomAuthlibInjectorProviderConfig.class)
                .orElse(null);
        
        if (config == null) {
            SkinRestorer.LOGGER.warn("Could not find config for custom provider '{}'", this.providerName);
            this.resolvedApiRoot = null;
            this.cacheConfig = null;
            return;
        }
        
        this.skinResigner.reload(config.useProviderSignature());
        this.resolveApiRoot(config.baseUrl());
        this.cacheConfig = config.cache();
        this.createSkinCache();
    }
    
    private void resolveApiRoot(String baseUrl) {
        var baseUri = WebUtils.parseUri(WebUtils.ensureTrailingSlash(baseUrl));
        if (baseUri == null)
            return;
        
        try {
            var request = HttpRequest.newBuilder()
                    .uri(baseUri)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            
            var response = WebUtils.executeRequest(request);
            
            var locationHeader = response.headers().firstValue("x-authlib-injector-api-location");
            if (locationHeader.isPresent()) {
                var redirected = WebUtils.parseUri(WebUtils.ensureTrailingSlash(locationHeader.get()));
                this.resolvedApiRoot = redirected != null ? redirected : baseUri;
            } else {
                this.resolvedApiRoot = baseUri;
            }
            
            SkinRestorer.LOGGER.info("Resolved authlib-injector API root for '{}': {}", this.providerName, this.resolvedApiRoot);
        } catch (Exception e) {
            SkinRestorer.LOGGER.warn("Failed to resolve authlib-injector API root for '{}', using baseUrl as-is", this.providerName, e);
            this.resolvedApiRoot = baseUri;
        }
    }
    
    @Override
    protected CacheConfig getCacheConfig() {
        return this.cacheConfig;
    }
    
    @Override
    protected void validate(String argument, SkinVariant variant) throws Exception {
        super.validate(argument, variant);
        if (this.resolvedApiRoot == null)
            throw new IllegalStateException("Custom provider '" + this.providerName + "' has no resolved API root");
    }
    
    @Override
    protected UUID lookupUuid(String username) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(this.resolvedApiRoot.resolve("api/profiles/minecraft"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("[\"" + username + "\"]"))
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        var profiles = JsonUtils.fromJson(response.body(), GameProfile[].class);
        if (profiles == null || profiles.length == 0)
            throw new IllegalArgumentException("no profile with name " + username);
        
        return profiles[0].getId();
    }
    
    @Override
    protected GameProfile fetchProfileWithProperties(UUID uuid) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(this.resolvedApiRoot
                        .resolve("sessionserver/session/minecraft/profile/")
                        .resolve(UndashedUuid.toString(uuid) + "?unsigned=false")
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with uuid " + uuid);
        
        return JsonUtils.fromJson(response.body(), MinecraftProfilePropertiesResponse.class).toProfile();
    }
    
    @Override
    protected Optional<Property> extractSkin(GameProfile profile) throws Exception {
        return this.skinResigner.extractSkin(profile);
    }
}
