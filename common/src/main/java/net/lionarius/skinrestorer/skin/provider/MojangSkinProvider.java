package net.lionarius.skinrestorer.skin.provider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.util.UndashedUuid;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.WebUtils;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class MojangSkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "mojang";
    
    private static final URI API_URI;
    private static final URI SESSION_SERVER_URI;
    
    public static final String PROFILE_CACHE_FILENAME = "mojang_profile_cache.json";
    private static final GameProfileCache PROFILE_CACHE;
    
    private static LoadingCache<UUID, Optional<Property>> SKIN_CACHE;
    
    static {
        try {
            API_URI = new URI("https://api.mojang.com");
            SESSION_SERVER_URI = new URI("https://sessionserver.mojang.com");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        
        PROFILE_CACHE = new GameProfileCache((names, callback) -> {
            for (var name : names) {
                try {
                    var profile = MojangSkinProvider.getProfile(name);
                    callback.onProfileLookupSucceeded(profile);
                } catch (IOException e) {
                    callback.onProfileLookupFailed(name, e);
                }
            }
        }, SkinRestorer.getConfigDir().resolve(PROFILE_CACHE_FILENAME).toFile());
    }
    
    public static void reload() {
        createCache();
    }
    
    private static void createCache() {
        var config = SkinRestorer.getConfig().providersConfig().mojang();
        var time = config.cache().enabled() ? config.cache().duration() : 0;
        
        SKIN_CACHE = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Optional<Property> load(@NotNull UUID key) throws Exception {
                        return MojangSkinProvider.loadSkin(key);
                    }
                });
    }
    
    public static SkinProviderContext skinProviderContextFromProfile(GameProfile gameProfile) {
        return new SkinProviderContext(MojangSkinProvider.PROVIDER_NAME, gameProfile.getName(), null);
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
            
            var cachedProfile = MojangSkinProvider.PROFILE_CACHE.get(username);
            if (cachedProfile.isEmpty())
                throw new IllegalArgumentException("no profile found for " + username);
            
            return Result.success(SKIN_CACHE.get(cachedProfile.get().getId()));
        } catch (UncheckedExecutionException e) {
            return Result.error((Exception) e.getCause());
        } catch (Exception e) {
            return Result.error(e);
        }
    }
    
    private static Optional<Property> loadSkin(UUID uuid) throws Exception {
        var profile = MojangSkinProvider.getProfileWithProperties(uuid);
        var textures = PlayerUtils.getPlayerSkin(profile);
        
        return Optional.ofNullable(textures);
    }
    
    private static GameProfile getProfile(final String name) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(MojangSkinProvider.API_URI
                        .resolve("/users/profiles/minecraft/")
                        .resolve(name)
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with name " + name);
        
        return JsonUtils.fromJson(response.body(), GameProfile.class);
    }
    
    private static GameProfile getProfileWithProperties(UUID uuid) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(MojangSkinProvider.SESSION_SERVER_URI
                        .resolve("/session/minecraft/profile/")
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
}
