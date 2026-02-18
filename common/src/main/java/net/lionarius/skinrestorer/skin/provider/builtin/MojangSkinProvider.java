package net.lionarius.skinrestorer.skin.provider.builtin;

import com.mojang.authlib.*;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.exception.TransparentException;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.skin.provider.YggdrasilSkinProvider;
import net.minecraft.server.players.CachedUserNameToIdResolver;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

public final class MojangSkinProvider extends YggdrasilSkinProvider {

    public static final String PROVIDER_NAME = "mojang";
    public static final String PROFILE_CACHE_FILENAME = "mojang_profile_cache.json";
    private static final Environment ENVIRONMENT;
    private static final URI SERVICES_SERVER_URI;
    private static final URI SESSION_SERVER_URI;

    private final CachedUserNameToIdResolver profileCache;

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
        this.createSkinCache();
    }

    @Override
    protected CacheConfig getCacheConfig() {
        return SkinRestorer.getConfig().providersConfig().mojang().cache();
    }

    public static SkinProviderContext skinProviderContextFromProfile(GameProfile gameProfile) {
        return new SkinProviderContext(MojangSkinProvider.PROVIDER_NAME, gameProfile.name(), null);
    }

    @Override
    protected UUID resolveUuid(String username) throws Exception {
        var cachedProfile = this.profileCache.get(username);
        if (cachedProfile.isEmpty())
            throw new IllegalArgumentException("no profile found for " + username);

        return cachedProfile.get().id();
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
