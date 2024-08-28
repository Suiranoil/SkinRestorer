package net.lionarius.skinrestorer.skin.provider;

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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.UUID;

public final class MojangSkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "mojang";
    
    private static final URI API_URI;
    private static final URI SESSION_SERVER_URI;
    
    public static final String CACHE_FILENAME = "mojang_profile_cache.json";
    private static final GameProfileCache PROFILE_CACHE;
    
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
        }, SkinRestorer.getConfigDir().resolve(CACHE_FILENAME).toFile());
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
    public Result<Optional<Property>, Exception> getSkin(String username, SkinVariant variant) {
        if (!StringUtil.isValidPlayerName(username))
            return Result.error(new IllegalArgumentException("invalid username"));
        
        try {
            var cachedProfile = MojangSkinProvider.PROFILE_CACHE.get(username);
            if (cachedProfile.isEmpty())
                throw new IllegalArgumentException("no profile found for " + username);
            
            var profile = MojangSkinProvider.getProfileWithProperties(cachedProfile.get().getId());
            var textures = PlayerUtils.getPlayerSkin(profile);
            
            return Result.ofNullable(textures);
        } catch (Exception e) {
            return Result.error(e);
        }
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
