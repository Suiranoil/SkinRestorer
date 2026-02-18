package net.lionarius.skinrestorer.skin.provider.custom;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import com.mojang.util.UndashedUuid;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.config.provider.custom.CustomAuthlibInjectorProviderConfig;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.skin.provider.SkinProviderParameterType;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.WebUtils;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class CustomAuthlibInjectorSkinProvider implements SkinProvider {
    private final String providerName;
    private final SkinSigner skinSigner;

    private LoadingCache<String, Optional<Property>> skinCache;
    private Cache<Integer, Property> signatureCache;

    private boolean useProviderSignature;
    private URI resolvedApiRoot;

    public CustomAuthlibInjectorSkinProvider(String providerName, SkinSigner skinSigner) {
        this.providerName = providerName;
        this.skinSigner = skinSigner;
        this.useProviderSignature = false;
    }

    @Override
    public String getProviderName() {
        return this.providerName;
    }

    @Override
    public SkinProviderParameterType getParameterType() {
        return SkinProviderParameterType.USERNAME;
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
    public void reload() {
        var config = SkinRestorer.getConfig()
                .providersConfig()
                .findCustomByName(this.providerName, CustomAuthlibInjectorProviderConfig.class)
                .orElse(null);

        if (config == null) {
            SkinRestorer.LOGGER.warn("Could not find config for custom provider '{}'", this.providerName);
            this.resolvedApiRoot = null;
            this.skinCache = null;
            this.signatureCache = null;
            return;
        }

        this.useProviderSignature = config.useProviderSignature();
        this.resolveApiRoot(config.baseUrl());
        this.createCache(config.cache());
    }

    private void resolveApiRoot(String baseUrl) {
        var baseUri = WebUtils.parseUri(WebUtils.ensureTrailingSlash(baseUrl));
        if (baseUri == null)
            return;

        try {
            var request = HttpRequest.newBuilder()
                    .uri(baseUri)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            var response = WebUtils.executeRequest(request);

            var locationHeader = response.headers().firstValue("x-authlib-injector-api-location");
            if (locationHeader.isPresent()) {
                var redirected = WebUtils.parseUri(WebUtils.ensureTrailingSlash(locationHeader.get()));
                this.resolvedApiRoot = redirected != null ? redirected : baseUri;
            } else {
                this.resolvedApiRoot = baseUri;
            }

            SkinRestorer.LOGGER.info("Resolved authlib-injector API root for '{}': {}", this.providerName, this.resolvedApiRoot);
        } catch (Exception e) {
            SkinRestorer.LOGGER.warn("Failed to resolve authlib-injector API root for '{}', using baseUrl as-is", this.providerName, e);
            this.resolvedApiRoot = baseUri;
        }
    }

    private void createCache(CacheConfig config) {
        var time = config.enabled() ? config.duration() : 0;

        this.skinCache = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Optional<Property> load(@NotNull String key) throws Exception {
                        return CustomAuthlibInjectorSkinProvider.this.loadSkin(key);
                    }
                });

        if (this.useProviderSignature) {
            this.signatureCache = null;
        } else {
            this.signatureCache = CacheBuilder.newBuilder()
                    .expireAfterAccess(24, TimeUnit.HOURS)
                    .maximumSize(1000)
                    .build();
        }
    }

    @Override
    public Result<Optional<Property>, Exception> fetchSkin(String username, SkinVariant variant) {
        try {
            if (!StringUtil.isValidPlayerName(username))
                throw new IllegalArgumentException("invalid username");

            if (this.skinCache == null)
                throw new IllegalStateException("Custom provider '" + this.providerName + "' is not initialized");

            if (this.resolvedApiRoot == null)
                throw new IllegalStateException("Custom provider '" + this.providerName + "' has no resolved API root");

            return Result.success(this.skinCache.get(username.toLowerCase(Locale.ROOT)));
        } catch (UncheckedExecutionException e) {
            return Result.error((Exception) e.getCause());
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    private Optional<Property> loadSkin(String username) throws Exception {
        var uuid = this.lookupUuid(username);
        var profile = this.fetchProfile(uuid);

        if (this.useProviderSignature) {
            var textures = PlayerUtils.getPlayerSkin(profile);
            return Optional.ofNullable(textures);
        }

        var skin = PlayerUtils.getPlayerSkin(profile);
        if (skin == null)
            return Optional.empty();

        if (PlayerUtils.getSkinUrl(skin) == null)
            return Optional.empty();

        var propertyHash = skin.value().hashCode();
        var cachedSignature = this.signatureCache == null ? null : this.signatureCache.getIfPresent(propertyHash);
        if (cachedSignature != null)
            return Optional.of(cachedSignature);

        var signed = this.skinSigner.signSkin(skin);
        signed.ifPresent(property -> {
            if (this.signatureCache != null)
                this.signatureCache.put(propertyHash, property);
        });

        return signed;
    }

    private UUID lookupUuid(String username) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(this.resolvedApiRoot.resolve("api/profiles/minecraft"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("[\"" + username + "\"]"))
                .build();

        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);

        var profiles = JsonUtils.fromJson(response.body(), NameAndId[].class);
        if (profiles == null || profiles.length == 0)
            throw new IllegalArgumentException("no profile with name " + username);

        return profiles[0].id();
    }

    private GameProfile fetchProfile(UUID uuid) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(this.resolvedApiRoot
                        .resolve("sessionserver/session/minecraft/profile/")
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
