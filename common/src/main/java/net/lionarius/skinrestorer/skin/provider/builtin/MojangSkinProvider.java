package net.lionarius.skinrestorer.skin.provider.builtin;

import com.mojang.authlib.*;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.exception.TransparentException;
import net.lionarius.skinrestorer.skin.provider.SkinProviderContext;
import net.lionarius.skinrestorer.skin.provider.base.YggdrasilSkinProvider;
import net.minecraft.server.players.GameProfileCache;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public final class MojangSkinProvider extends YggdrasilSkinProvider {
    public static final String PROVIDER_NAME = "mojang";
    public static final String PROFILE_CACHE_FILENAME = "mojang_profile_cache.json";
    private static final Environment ENVIRONMENT;
    private static final URI SERVICES_SERVER_URI;
    private static final URI SESSION_SERVER_URI;

    private final GameProfileCache profileCache;

    static {
        try {
            ENVIRONMENT =
                    EnvironmentParser.getEnvironmentFromProperties().orElse(YggdrasilEnvironment.PROD.getEnvironment());

            SERVICES_SERVER_URI = new URI(ENVIRONMENT.getServicesHost());
            SESSION_SERVER_URI = new URI(ENVIRONMENT.getSessionHost());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public MojangSkinProvider() {
        this.profileCache = new GameProfileCache(
                (names, agent, callback) -> {
                    for (var name : names) {
                        try {
                            var profile = MojangSkinProvider.this.getProfile(name);
                            callback.onProfileLookupSucceeded(profile);
                        } catch (IOException e) {
                            throw new TransparentException(e);
                        }
                    }
                },
                SkinRestorer.getConfigDir().resolve(PROFILE_CACHE_FILENAME).toFile());
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
        return SkinRestorer.getConfig().providers().mojang().cache();
    }

    public static SkinProviderContext skinProviderContextFromProfile(GameProfile gameProfile) {
        return new SkinProviderContext(MojangSkinProvider.PROVIDER_NAME, gameProfile.getName(), null);
    }

    @Override
    protected UUID resolveUuid(String username) throws Exception {
        var cachedProfile = this.profileCache.get(username);
        if (cachedProfile.isEmpty()) throw new IllegalArgumentException("no profile found for " + username);

        return cachedProfile.get().getId();
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
