package net.lionarius.skinrestorer.skin.provider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.Pair;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.mineskin.Java11RequestHandler;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.Result;
import net.lionarius.skinrestorer.util.WebUtils;
import org.jetbrains.annotations.NotNull;
import org.mineskin.MineSkinClient;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.QueueResponse;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class MineskinSkinProvider implements SkinProvider {
    
    public static final String PROVIDER_NAME = "web";
    
    private static final URI API_URI;
    private static MineSkinClient MINESKIN_CLIENT;
    
    private static LoadingCache<Pair<URI, SkinVariant>, Optional<Property>> SKIN_CACHE;
    
    static {
        try {
            API_URI = new URI("https://api.mineskin.org");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static void reload() {
        var config = SkinRestorer.getConfig();
        var configApiKey = config.providersConfig().mineskin().apiKey();
        
        MINESKIN_CLIENT = MineSkinClient
                .builder()
                .userAgent(WebUtils.USER_AGENT)
                .gson(JsonUtils.GSON)
                .timeout((int) Duration.ofSeconds(config.requestTimeout()).toMillis())
                .requestHandler((userAgent, apiKey, timeout, gson) -> new Java11RequestHandler(
                        userAgent,
                        apiKey,
                        timeout,
                        gson,
                        SkinRestorer.getConfig().proxy().map(proxy -> new InetSocketAddress(proxy.host(), proxy.port())).orElse(null)
                ))
                .apiKey(configApiKey.isEmpty() ? null : configApiKey)
                .build();
        
        createCache();
    }
    
    private static void createCache() {
        var config = SkinRestorer.getConfig().providersConfig().mineskin();
        var time = config.cache().enabled() ? config.cache().duration() : 0;
        
        SKIN_CACHE = CacheBuilder.newBuilder()
                .expireAfterWrite(time, TimeUnit.SECONDS)
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
        } catch (UncheckedExecutionException e) {
            return Result.error((Exception) e.getCause());
        } catch (Exception e) {
            return Result.error(e);
        }
    }
    
    private static Optional<Property> loadSkin(URI uri, SkinVariant variant) throws Exception {
        var mineskinVariant = switch (variant) {
            case CLASSIC -> Variant.CLASSIC;
            case SLIM -> Variant.SLIM;
        };
        
        var request = GenerateRequest.url(uri)
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
