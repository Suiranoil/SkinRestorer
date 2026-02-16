package net.lionarius.skinrestorer.skin.provider.builtin;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.*;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.exception.TransparentException;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.skin.provider.YggdrasilSkinProvider;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.minecraft.server.players.CachedUserNameToIdResolver;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class MojangSkinProvider extends YggdrasilSkinProvider {
    
    public static final String PROVIDER_NAME = "mojang";
    public static final String PROFILE_CACHE_FILENAME = "mojang_profile_cache.json";
    private static final Environment ENVIRONMENT;
    private static final URI SERVICES_SERVER_URI;
    private static final URI SESSION_SERVER_URI;
    
    private final CachedUserNameToIdResolver profileCache;
    private LoadingCache<UUID, Optional<Property>> skinCache;
    
    static {
        try {
            ENVIRONMENT = EnvironmentParser.getEnvironmentFromProperties().orElse(YggdrasilEnvironment.PROD.getEnvironment());
            
            SERVICES_SERVER_URI = new URI(ENVIRONMENT.servicesHost());
            SESSION_SERVER_URI = new URI(ENVIRONMENT.sessionHost());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public MojangSkinProvider() {
        this.profileCache = new CachedUserNameToIdResolver(new GameProfileRepository() {
            @Override
            public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
                for (var name : names) {
                    try {
                        var profile = MojangSkinProvider.this.getProfile(name);
                        callback.onProfileLookupSucceeded(profile.name(), profile.id());
                    } catch (IOException e) {
                        throw new TransparentException(e);
                    }
                }
            }
            
            @Override
            public Optional<NameAndId> findProfileByName(String name) {
                try {
                    var profile = MojangSkinProvider.this.getProfile(name);
                    return Optional.of(new NameAndId(profile.id(), profile.name()));
                } catch (IOException e) {
                    throw new TransparentException(e);
                }
            }
        }, SkinRestorer.getConfigDir().resolve(PROFILE_CACHE_FILENAME).toFile());
    }
    
    @Override
    public String getProviderName() {
        return MojangSkinProvider.PROVIDER_NAME;
    }
    
    @Override
    public void reload() {
        this.createCache();
    }
    
    private void createCache() {
        var config = SkinRestorer.getConfig().providersConfig().mojang();
        var time = config.cache().enabled() ? config.cache().duration() : 0;
        
        this.skinCache = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Optional<Property> load(@NotNull UUID key) throws Exception {
                        return MojangSkinProvider.this.loadSkin(key);
                    }
                });
    }
    
    public static SkinProviderContext skinProviderContextFromProfile(GameProfile gameProfile) {
        return new SkinProviderContext(MojangSkinProvider.PROVIDER_NAME, gameProfile.name(), null);
    }
    
    @Override
    protected Optional<Property> fetchSkinImpl(String username) throws Exception {
        var cachedProfile = this.profileCache.get(username);
        if (cachedProfile.isEmpty())
            throw new IllegalArgumentException("no profile found for " + username);
        
        return this.skinCache.get(cachedProfile.get().id());
    }
    
    private Optional<Property> loadSkin(UUID uuid) throws Exception {
        var profile = this.getProfileWithProperties(uuid);
        var textures = PlayerUtils.getPlayerSkin(profile);
        
        return Optional.ofNullable(textures);
    }
    
    @Override
    protected URI baseSessionServerUrl() {
        return MojangSkinProvider.SESSION_SERVER_URI;
    }
    
    @Override
    protected URI baseServicesServerUrl() {
        return MojangSkinProvider.SERVICES_SERVER_URI;
    }
}
