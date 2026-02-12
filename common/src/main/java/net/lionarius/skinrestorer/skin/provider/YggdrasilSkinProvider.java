package net.lionarius.skinrestorer.skin.provider;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import com.mojang.util.UndashedUuid;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.WebUtils;
import net.minecraft.util.StringUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.UUID;

public abstract class YggdrasilSkinProvider implements SkinProvider {
    
    protected abstract URI baseSessionServerUrl();
    
    protected abstract URI baseServicesServerUrl();
    
    protected abstract Optional<Property> fetchSkinImpl(String username) throws Exception;
    
    @Override
    public boolean hasVariantSupport() {
        return false;
    }
    
    @Override
    public String getArgumentName() {
        return "username";
    }
    
    @Override
    public Result<Optional<Property>, Exception> fetchSkin(String username, SkinVariant variant) {
        try {
            if (!StringUtil.isValidPlayerName(username))
                throw new IllegalArgumentException("invalid username");
            
            return Result.success(this.fetchSkinImpl(username));
        } catch (UncheckedExecutionException e) {
            return Result.error((Exception) e.getCause());
        } catch (Exception e) {
            return Result.error(e);
        }
    }
    
    protected NameAndId getProfile(final String name) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(this.baseServicesServerUrl()
                        .resolve("/minecraft/profile/lookup/name/")
                        .resolve(name)
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with name " + name);
        
        return JsonUtils.fromJson(response.body(), NameAndId.class);
    }
    
    protected GameProfile getProfileWithProperties(UUID uuid) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(this.baseSessionServerUrl()
                        .resolve("/session/minecraft/profile/")
                        .resolve(UndashedUuid.toString(uuid) + "?unsigned=false")
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with uuid " + uuid);
        
        return JsonUtils.fromJson(response.body(), MinecraftProfilePropertiesResponse.class).profile();
    }
}
