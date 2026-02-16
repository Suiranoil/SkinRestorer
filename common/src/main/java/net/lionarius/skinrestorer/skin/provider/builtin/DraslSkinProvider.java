package net.lionarius.skinrestorer.skin.provider.builtin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.skin.provider.YggdrasilSkinProvider;
import net.lionarius.skinrestorer.util.PlayerUtils;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class DraslSkinProvider extends YggdrasilSkinProvider {
    
    public static final String PROVIDER_NAME = "drasl";
    
    private final SkinSigner skinSigner;
    
    private LoadingCache<String, Optional<Property>> skinCache;
    private Cache<Integer, Property> signatureCache;
    
    private URI baseUrl;
    
    public DraslSkinProvider(SkinSigner skinSigner) {
        this.skinSigner = skinSigner;
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
    protected Optional<Property> fetchSkinImpl(String username) throws Exception {
        var usernameLowerCase = username.toLowerCase();
        
        return this.skinCache.get(usernameLowerCase);
    }
    
    private Optional<Property> loadSkin(String username) throws Exception {
        var nameAndId = getProfile(username);
        var profile = getProfileWithProperties(nameAndId.id());
        var skin = PlayerUtils.getPlayerSkin(profile);
        if (skin == null)
            return Optional.empty();

        if (PlayerUtils.getSkinUrl(skin) == null)
            return Optional.empty();

        var propertyHash = skin.value().hashCode();
        var cachedSignature = this.signatureCache.getIfPresent(propertyHash);
        
        if (cachedSignature != null) {
            return Optional.of(cachedSignature);
        }
        
        var signed = this.skinSigner.signSkin(skin);
        signed.ifPresent(prop -> this.signatureCache.put(propertyHash, prop));
        
        return signed;
    }
    
    @Override
    protected URI baseSessionServerUrl() {
        return this.baseUrl;
    }
    
    @Override
    protected URI baseServicesServerUrl() {
        return this.baseUrl;
    }
}
