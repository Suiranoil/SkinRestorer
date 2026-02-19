package net.lionarius.skinrestorer.skin.provider.builtin;

import com.mojang.authlib.properties.Property;
import it.unimi.dsi.fastutil.Pair;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.config.provider.CacheConfig;
import net.lionarius.skinrestorer.mineskin.Java11RequestHandler;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinProviderParameterType;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.skin.provider.base.AbstractSkinProvider;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.WebUtils;
import org.jetbrains.annotations.Nullable;
import org.mineskin.MineSkinClient;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.QueueResponse;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public final class MineskinSkinProvider extends AbstractSkinProvider<Pair<URI, SkinVariant>> implements SkinSigner {
    
    public static final String PROVIDER_NAME = "web";
    
    private MineSkinClient mineskinClient;
    
    @Override
    public void reload() {
        this.reloadClient();
        this.createSkinCache();
    }
    
    private void reloadClient() {
        var config = SkinRestorer.getConfig();
        var configApiKey = config.providersConfig().mineskin().apiKey();
        
        this.mineskinClient = MineSkinClient
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
                .apiKey(configApiKey.isEmpty() ? null : configApiKey)
                .build();
    }
    
    @Override
    public String getProviderName() {
        return MineskinSkinProvider.PROVIDER_NAME;
    }
    
    @Override
    public SkinProviderParameterType getParameterType() {
        return SkinProviderParameterType.CUSTOM;
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
    protected CacheConfig getCacheConfig() {
        return SkinRestorer.getConfig().providersConfig().mineskin().cache();
    }
    
    @Override
    protected Pair<URI, SkinVariant> getCacheKey(String argument, SkinVariant variant) throws Exception {
        return Pair.of(new URI(argument), variant);
    }
    
    @Override
    protected Optional<Property> loadSkin(Pair<URI, SkinVariant> key) throws Exception {
        return this.loadSkin(key.first(), key.second());
    }
    
    @Override
    public Optional<Property> signSkin(URI uri, SkinVariant variant) throws Exception {
        return this.loadSkin(uri, variant);
    }
    
    @Override
    public Optional<Property> signSkin(Property property) throws Exception {
        var skin = PlayerUtils.getSkinUrl(property);
        if (skin == null)
            return Optional.empty();
        
        return this.loadSkin(new URI(skin.first()), skin.second());
    }
    
    private Optional<Property> loadSkin(URI uri, @Nullable SkinVariant variant) throws Exception {
        var mineskinVariant = switch (variant) {
            case CLASSIC -> Variant.CLASSIC;
            case SLIM -> Variant.SLIM;
            case null -> Variant.AUTO;
        };
        
        var request = "file".equals(uri.getScheme())
                ? GenerateRequest.upload(Files.newInputStream(Path.of(uri)))
                .variant(mineskinVariant)
                .name("skinrestorer-skin")
                .visibility(Visibility.UNLISTED)
                : GenerateRequest.url(uri)
                .variant(mineskinVariant)
                .name("skinrestorer-skin")
                .visibility(Visibility.UNLISTED);
        
        var skin = this.mineskinClient.queue().submit(request)
                .thenApply(QueueResponse::getJob)
                .thenCompose(jobInfo -> jobInfo.waitForCompletion(this.mineskinClient))
                .thenCompose(jobReference -> jobReference.getOrLoadSkin(this.mineskinClient))
                .join();
        
        return Optional.of(new Property(
                PlayerUtils.TEXTURES_KEY,
                skin.texture().data().value(),
                skin.texture().data().signature()
        ));
    }
}
