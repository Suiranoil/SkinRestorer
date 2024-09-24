package net.lionarius.skinrestorer.skin.provider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class ElyBySkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "ely.by";
    
    private static final URI API_URI;
    
    private static LoadingCache<String, Optional<Property>> SKIN_CACHE;
    
    static {
        try {
            API_URI = new URI("http://skinsystem.ely.by");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static void createCache() {
        var config = SkinRestorer.getConfig().providersConfig().ely_by();
        var time = config.cache().enabled() ? config.cache().duration() : 0;
        
        SKIN_CACHE = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Optional<Property> load(@NotNull String key) throws Exception {
                        return ElyBySkinProvider.loadSkin(key);
                    }
                });
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
            if (!StringUtils.isValidPlayerName(username))
                throw new IllegalArgumentException("invalid username");
            
            var usernameLowerCase = username.toLowerCase(Locale.ROOT);
            
            return Result.success(SKIN_CACHE.get(usernameLowerCase));
        } catch (UncheckedExecutionException e) {
            return Result.error((Exception) e.getCause());
        } catch (Exception e) {
            return Result.error(e);
        }
    }
    
    private static Optional<Property> loadSkin(String username) throws Exception {
        var profile = ElyBySkinProvider.getElyByProfile(username);
        var textures = PlayerUtils.getPlayerSkin(profile);
        
        return Optional.ofNullable(textures);
    }
    
    private static GameProfile getElyByProfile(String username) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(ElyBySkinProvider.API_URI
                        .resolve("/textures/signed/")
                        .resolve(username + "?unsigned=false")
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with name " + username);
        
        return JsonUtils.fromJson(response.body(), MinecraftProfilePropertiesResponse.class).toProfile();
    }
}
