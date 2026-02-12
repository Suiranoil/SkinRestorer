package net.lionarius.skinrestorer.skin.provider;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.WebUtils;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class DraslSkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "drasl";
    
    private final MineskinSkinProvider mineskinProvider;
    
    private LoadingCache<String, Optional<Property>> skinCache;
    private Cache<String, Property> signatureCache;
    
    private URI baseUrl;
    
    public DraslSkinProvider(MineskinSkinProvider mineskinProvider) {
        this.mineskinProvider = mineskinProvider;
    }
    
    @Override
    public String getProviderName() {
        return DraslSkinProvider.PROVIDER_NAME;
    }
    
    @Override
    public void reload() {
        this.reloadUrl();
        this.createCache();
    }
    
    private void reloadUrl() {
        var config = SkinRestorer.getConfig();
        var draslUrl = config.providersConfig().drasl().url();
        
        if (draslUrl != null && !draslUrl.isEmpty()) {
            try {
                this.baseUrl = new URI(draslUrl);
            } catch (URISyntaxException e) {
                SkinRestorer.LOGGER.warn("Invalid Drasl base URL: {}", draslUrl, e);
            }
        }
    }
    
    private void createCache() {
        var config = SkinRestorer.getConfig().providersConfig().drasl();
        var time = config.cache().enabled() ? config.cache().duration() : 0;
        
        this.skinCache = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Optional<Property> load(@NotNull String key) throws Exception {
                        return DraslSkinProvider.this.loadSkin(key);
                    }
                });
        
        this.signatureCache = CacheBuilder.newBuilder()
                .expireAfterAccess(24, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();
    }
    
    @Override
    public String getArgumentName() {
        return "username";
    }
    
    @Override
    public boolean hasVariantSupport() {
        return false;
    }
    
    @Override
    public Result<Optional<Property>, Exception> fetchSkin(String username, SkinVariant variant) {
        try {
            if (!StringUtil.isValidPlayerName(username))
                throw new IllegalArgumentException("invalid username");
            
            var usernameLowerCase = username.toLowerCase();
            
            return Result.success(this.skinCache.get(usernameLowerCase));
        } catch (UncheckedExecutionException e) {
            return Result.error((Exception) e.getCause());
        } catch (Exception e) {
            return Result.error(e);
        }
    }
    
    private Optional<Property> loadSkin(String username) throws Exception {
        var nameAndId = getProfile(username);
        var profile = getProfileWithProperties(nameAndId.id());
        var skin = PlayerUtils.getSkinUrl(profile);
        
        if (skin == null)
            return Optional.empty();
        
        var textureUrl = skin.first();
        var variant = skin.second();
        
        var cachedSignature = this.signatureCache.getIfPresent(textureUrl);
        
        if (cachedSignature != null) {
            return Optional.of(cachedSignature);
        }
        
        var signed = this.mineskinProvider.loadSkin(new URI(textureUrl), variant);
        signed.ifPresent(prop -> this.signatureCache.put(textureUrl, prop));
        
        return signed;
    }
    
    private NameAndId getProfile(final String name) throws IOException {
        if (this.baseUrl == null)
            throw new IllegalStateException("Drasl is not configured in this server.");
        
        var request = HttpRequest.newBuilder()
                .uri(this.baseUrl
                        .resolve("/minecraft/profile/lookup/name/")
                        .resolve(name)
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with name " + name);
        
        return JsonUtils.fromJson(response.body(), NameAndId.class);
    }
    
    private GameProfile getProfileWithProperties(UUID uuid) throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(this.baseUrl
                        .resolve("/session/minecraft/profile/")
                        .resolve(uuid.toString().replace("-", ""))
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with uuid " + uuid);
        
        return JsonUtils.fromJson(response.body(), MinecraftProfilePropertiesResponse.class).profile();
    }
}
