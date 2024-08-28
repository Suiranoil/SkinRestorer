package net.lionarius.skinrestorer.skin.provider;

import com.google.gson.JsonObject;
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

public final class ElyBySkinProvider implements SkinProvider {
    public static final String PROVIDER_NAME = "ely.by";
    
    private static final URI API_URI;
    
    static {
        try {
            API_URI = new URI("http://skinsystem.ely.by/");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
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
            var profile = ElyBySkinProvider.getElyByProfile(username);
            
            var properties = profile.getAsJsonArray("properties");
            var textures = PlayerUtils.findTexturesProperty(properties);
            
            return Result.ofNullable(textures);
        } catch (Exception e) {
            return Result.error(e);
        }
    }
    
    private static JsonObject getElyByProfile(String username) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(ElyBySkinProvider.API_URI
                        .resolve("textures/signed/")
                        .resolve(username + "?unsigned=false")
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with name " + username);
        
        return JsonUtils.parseJson(response.body());
    }
}
