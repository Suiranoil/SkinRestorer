package net.lionarius.skinrestorer.skin.provider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import com.mojang.util.UndashedUuid;
import it.unimi.dsi.fastutil.Pair;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.mineskin.Java11RequestHandler;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProvider;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.WebUtils;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.mineskin.MineSkinClient;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.QueueResponse;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.io.IOException;

public final class DraslSkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "drasl";
    
    private static MineSkinClient MINESKIN_CLIENT;
    
    private static LoadingCache<String, Optional<Property>> SKIN_CACHE;
    
    private static URI BASE_URL;
    
    public static void reload() {
        var config = SkinRestorer.getConfig();
        var mineskinApiKey = config.providersConfig().mineskin().apiKey();
        var draslConfig = config.providersConfig().drasl();
        var draslUrl = draslConfig.url();

        if (draslUrl != null && !draslUrl.isEmpty()) {
            try {
                BASE_URL = new URI(draslUrl);
            } catch (URISyntaxException e) {
                SkinRestorer.LOGGER.warn("Invalid Drasl base URL: {}", draslUrl, e);
            }
        }

        MINESKIN_CLIENT = MineSkinClient
                .builder()
                .userAgent(WebUtils.USER_AGENT)
                .gson(JsonUtils.GSON)
                .timeout((int) Duration.ofSeconds(config.requestTimeout()).toMillis())
                .requestHandler((baseUrl, userAgent, apiKey, timeout, gson) -> new Java11RequestHandler(
                        baseUrl,
                        userAgent,
                        apiKey,
                        timeout,
                        gson,
                        SkinRestorer.getConfig().proxy().map(proxy -> new InetSocketAddress(proxy.host(), proxy.port())).orElse(null)
                ))
                .apiKey(mineskinApiKey.isEmpty() ? null : mineskinApiKey)
                .build();
        
        createCache();
    }
    
    private static void createCache() {
        var config = SkinRestorer.getConfig().providersConfig().drasl();
        var time = config.cache().enabled() ? config.cache().duration() : 0;
        
        SKIN_CACHE = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Optional<Property> load(@NotNull String key) throws Exception {
                        return DraslSkinProvider.loadSkin(key);
                    }
                });
    }
    
    @Override
    public String getArgumentName() {
        return "username";
    }
    
    @Override
    public boolean hasVariantSupport() {
        return true;
    }
    
    @Override
    public Result<Optional<Property>, Exception> fetchSkin(String username, SkinVariant variant) {
        try {
            if (!StringUtil.isValidPlayerName(username))
                throw new IllegalArgumentException("invalid username");
            
            var usernameLowerCase = username.toLowerCase();
            
            return Result.success(SKIN_CACHE.get(username));
        } catch (UncheckedExecutionException e) {
            return Result.error((Exception) e.getCause());
        } catch (Exception e) {
            return Result.error(e);
        }
    }
    
    private static Optional<Property> loadSkin(String username) throws Exception {
        var nameAndId = getProfile(username);
        var profile = getProfileWithProperties(nameAndId.id());
        var skin = PlayerUtils.getSkinUrl(profile);
        
        if (skin == null)
            return Optional.empty();
        
        return DraslSkinProvider.signSkinUrl(skin.first(), skin.second());
    }
    
    private static NameAndId getProfile(final String name) throws IOException {
        if (BASE_URL == null)
            throw new IllegalStateException("Drasl is not configured, check your config.");
        
        var request = HttpRequest.newBuilder()
                .uri(DraslSkinProvider.BASE_URL
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
    
    private static com.mojang.authlib.GameProfile getProfileWithProperties(UUID uuid) throws Exception {
        if (BASE_URL == null)
            throw new IllegalStateException("Drasl BASE_URL is not initialized. Make sure the Drasl provider is properly configured and loaded.");
        
        var request = HttpRequest.newBuilder()
                .uri(DraslSkinProvider.BASE_URL
                        .resolve("/session/minecraft/profile/")
                        .resolve(uuid.toString().replace("-", ""))
                )
                .GET()
                .build();
        
        var response = WebUtils.executeRequest(request);
        WebUtils.throwOnClientErrors(response);
        
        if (response.statusCode() != 200)
            throw new IllegalArgumentException("no profile with uuid " + uuid);
        
        return JsonUtils.fromJson(response.body(), MinecraftProfilePropertiesResponse.class).profile();
    }
    
    private static Optional<Property> signSkinUrl(String textureUrl, SkinVariant variant) throws Exception {
        var mineskinVariant = switch (variant) {
            case CLASSIC -> Variant.CLASSIC;
            case SLIM -> Variant.SLIM;
        };
        
        var request = GenerateRequest.url(new URI(textureUrl))
                .variant(mineskinVariant)
                .name("skinrestorer-skin")
                .visibility(Visibility.UNLISTED);
        
        var skin = MINESKIN_CLIENT.queue().submit(request)
                .thenApply(QueueResponse::getJob)
                .thenCompose(jobInfo -> jobInfo.waitForCompletion(MINESKIN_CLIENT))
                .thenCompose(jobReference -> jobReference.getOrLoadSkin(MINESKIN_CLIENT))
                .join();
        
        return Optional.of(new Property(
                PlayerUtils.TEXTURES_KEY,
                skin.texture().data().value(),
                skin.texture().data().signature()
        ));
    }
}
