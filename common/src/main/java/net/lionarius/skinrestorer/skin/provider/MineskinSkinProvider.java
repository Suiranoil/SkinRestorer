package net.lionarius.skinrestorer.skin.provider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.Pair;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.WebUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class MineskinSkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "web";
    
    private static final URI API_URI;
    
    private static final LoadingCache<Pair<URI, SkinVariant>, Optional<Property>> SKIN_CACHE;
    
    static {
        try {
            API_URI = new URI("https://api.mineskin.org");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        
        SKIN_CACHE = CacheBuilder.newBuilder()
                .expireAfterWrite(300, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Optional<Property> load(@NotNull Pair<URI, SkinVariant> key) throws Exception {
                        return MineskinSkinProvider.loadSkin(key.first(), key.second());
                    }
                });
    }
    
    @Override
    public String getArgumentName() {
        return "url";
    }
    
    @Override
    public boolean hasVariantSupport() {
        return true;
    }
    
    @Override
    public Result<Optional<Property>, Exception> fetchSkin(String url, SkinVariant variant) {
        try {
            var uri = new URI(url);
            
            return Result.success(SKIN_CACHE.get(Pair.of(uri, variant)));
        } catch (Exception e) {
            return Result.error(e);
        }
    }
    
    private static Optional<Property> loadSkin(URI uri, SkinVariant variant) throws Exception {
        var result = MineskinSkinProvider.uploadToMineskin(uri, variant);
        var texture = result.getAsJsonObject("data").getAsJsonObject("texture");
        var textures = new Property(PlayerUtils.TEXTURES_KEY, texture.get("value").getAsString(), texture.get("signature").getAsString());
        
        return Optional.of(textures);
    }
    
    private static JsonObject uploadToMineskin(URI url, SkinVariant variant) throws IOException {
        var body = ("{\"variant\":\"%s\",\"name\":\"%s\",\"visibility\":%d,\"url\":\"%s\"}")
                .formatted(variant.toString(), "none", 0, url);
        
        var request = HttpRequest.newBuilder()
                .uri(MineskinSkinProvider.API_URI.resolve("/generate/url"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("could not get mineskin skin");
        
        return JsonUtils.parseJson(response.body());
    }
}
