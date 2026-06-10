package net.lionarius.skinrestorer.skin.provider.builtin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.skin.provider.base.UsernameSkinProvider;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.WebUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Optional;

public final class ElyBySkinProvider extends UsernameSkinProvider {
    public static final String PROVIDER_NAME = "ely.by";

    private static final URI API_URI;

    static {
        try {
            API_URI = new URI("https://skinsystem.ely.by");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void reload() {
        this.createSkinCache();
    }

    @Override
    public String getProviderName() {
        return ElyBySkinProvider.PROVIDER_NAME;
    }

    @Override
    protected CacheConfig getCacheConfig() {
        return SkinRestorer.getConfig().providers().ely_by().cache();
    }

    @Override
    protected Optional<Property> loadSkin(String username) throws Exception {
        var profile = ElyBySkinProvider.getElyByProfile(username);
        var textures = PlayerUtils.getPlayerSkin(profile);

        return Optional.ofNullable(textures);
    }

    private static GameProfile getElyByProfile(String username) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(API_URI.resolve("/textures/signed/").resolve(WebUtils.urlEncode(username) + "?unsigned=false"))
                .GET()
                .build();

        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);

        if (response.statusCode() != 200) throw new IllegalArgumentException("no profile with name " + username);

        return JsonUtils.fromJson(response.body(), MinecraftProfilePropertiesResponse.class)
                .profile();
    }
}
