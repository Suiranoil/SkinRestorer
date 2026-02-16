package net.lionarius.skinrestorer.skin.provider.builtin;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
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
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class ElyBySkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "ely.by";
    
    private static final URI API_URI;
    
    private LoadingCache<String, Optional<Property>> skinCache;
    
    static {
        try {
            API_URI = new URI("http://skinsystem.ely.by");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    @Override
    public void reload() {
        this.createCache();
    }
    
    private void createCache() {
        var config = SkinRestorer.getConfig().providersConfig().ely_by();
        var time = config.cache().enabled() ? config.cache().duration() : 0;
        
        this.skinCache = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Optional<Property> load(@NotNull String key) throws Exception {
                        return ElyBySkinProvider.this.loadSkin(key);
                    }
                });
    }
    
    @Override
    public String getProviderName() {
        return ElyBySkinProvider.PROVIDER_NAME;
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
            
            var usernameLowerCase = username.toLowerCase(Locale.ROOT);
            
            return Result.success(this.skinCache.get(usernameLowerCase));
        } catch (UncheckedExecutionException e) {
            return Result.error((Exception) e.getCause());
        } catch (Exception e) {
            return Result.error(e);
        }
    }
    
    private Optional<Property> loadSkin(String username) throws Exception {
        var profile = ElyBySkinProvider.getElyByProfile(username);
        var textures = PlayerUtils.getPlayerSkin(profile);
        
        return Optional.ofNullable(textures);
    }
    
    private static GameProfile getElyByProfile(String username) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(API_URI
                        .resolve("/textures/signed/")
                        .resolve(username + "?unsigned=false")
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with name " + username);
        
        return JsonUtils.fromJson(response.body(), MinecraftProfilePropertiesResponse.class).profile();
    }
}
