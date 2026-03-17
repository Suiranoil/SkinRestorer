package net.lionarius.skinrestorer.skin.provider.base;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.util.UndashedUuid;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.WebUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.UUID;

public abstract class YggdrasilSkinProvider extends ProfileSkinProvider {

    protected abstract URI baseSessionServerUrl();

    protected abstract URI baseServicesServerUrl();

    @Override
    protected UUID lookupUuid(String username) throws Exception {
        return this.getProfile(username).getId();
    }

    @Override
    protected GameProfile fetchProfileWithProperties(UUID uuid) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(this.baseSessionServerUrl()
                        .resolve("/session/minecraft/profile/")
                        .resolve(UndashedUuid.toString(uuid) + "?unsigned=false"))
                .GET()
                .build();

        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);

        if (response.statusCode() != 200) throw new IllegalArgumentException("no profile with uuid " + uuid);

        return JsonUtils.fromJson(response.body(), MinecraftProfilePropertiesResponse.class).toProfile();
    }

    protected GameProfile getProfile(final String name) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(this.baseServicesServerUrl()
                        .resolve("/minecraft/profile/lookup/name/")
                        .resolve(name))
                .GET()
                .build();

        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);

        if (response.statusCode() != 200) throw new IllegalArgumentException("no profile with name " + name);

        return JsonUtils.fromJson(response.body(), GameProfile.class);
    }
}
