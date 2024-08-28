package net.lionarius.skinrestorer.skin.provider;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.WebUtils;
import net.minecraft.util.StringUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Optional;

public final class MojangSkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "mojang";
    
    private static final URI API_URI;
    private static final URI SESSION_SERVER_URI;
    
    static {
        try {
            API_URI = new URI("https://api.mojang.com/");
            SESSION_SERVER_URI = new URI("https://sessionserver.mojang.com/");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
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
            var uuid = MojangSkinProvider.getUuid(username);
            var profile = MojangSkinProvider.getMojangProfile(uuid);
            
            var properties = profile.getAsJsonArray("properties");
            var textures = PlayerUtils.findTexturesProperty(properties);
            
            return Result.ofNullable(textures);
        } catch (Exception e) {
            return Result.error(e);
        }
    }
    
    private static String getUuid(final String name) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(MojangSkinProvider.API_URI
                        .resolve("users/profiles/minecraft/")
                        .resolve(name)
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with name " + name);
        
        var profile = JsonUtils.parseJson(response.body());
        if (profile == null)
            return null;
        
        return profile.get("id").getAsString();
    }
    
    private static JsonObject getMojangProfile(String uuid) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(MojangSkinProvider.SESSION_SERVER_URI
                        .resolve("session/minecraft/profile/")
                        .resolve(uuid + "?unsigned=false")
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with uuid " + uuid);
        
        return JsonUtils.parseJson(response.body());
    }
}
