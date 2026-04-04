package net.lionarius.skinrestorer.mineskin;

import com.mojang.authlib.properties.Property;
import net.lionarius.skinrestorer.SkinRestorer;
import net.lionarius.skinrestorer.skin.SkinVariant;
import net.lionarius.skinrestorer.skin.provider.SkinSigner;
import net.lionarius.skinrestorer.util.JsonUtils;
import net.lionarius.skinrestorer.util.PlayerUtils;
import net.lionarius.skinrestorer.util.WebUtils;
import org.jetbrains.annotations.Nullable;
import org.mineskin.MineSkinClient;
import org.mineskin.data.Variant;
import org.mineskin.data.Visibility;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.QueueResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

public final class MineskinService implements SkinSigner {
    public static final MineskinService INSTANCE = new MineskinService();
    private static final String SKIN_NAME = "skinrestorer-skin";

    private MineSkinClient mineskinClient;
    private boolean proxyUrlUpload;

    private MineskinService() {}

    public void reload() {
        var config = SkinRestorer.getConfig();
        var mineskinConfig = config.providers().mineskin();
        var configApiKey = mineskinConfig.apiKey();

        this.proxyUrlUpload = mineskinConfig.proxyUrlUpload();
        this.mineskinClient = MineSkinClient.builder()
                .userAgent(WebUtils.getUserAgent())
                .gson(JsonUtils.GSON)
                .timeout((int) Duration.ofSeconds(config.request().timeout()).toMillis())
                .requestHandler((baseUrl, userAgent, apiKey, timeout, gson) -> new Java11RequestHandler(
                        baseUrl,
                        userAgent,
                        apiKey,
                        timeout,
                        gson,
                        SkinRestorer.getConfig()
                                .request()
                                .proxy()
                                .map(proxy -> new InetSocketAddress(proxy.host(), proxy.port()))
                                .orElse(null)))
                .apiKey(configApiKey.isEmpty() ? null : configApiKey)
                .build();
    }

    @Override
    public Optional<Property> signSkin(URI uri, SkinVariant variant) throws Exception {
        return this.generateSkin(uri, variant);
    }

    @Override
    public Optional<Property> signSkin(Property property) throws Exception {
        var skin = PlayerUtils.getSkinUrl(property);
        if (skin == null) return Optional.empty();

        return this.generateSkin(new URI(skin.first()), skin.second());
    }

    private Optional<Property> generateSkin(URI uri, @Nullable SkinVariant variant) throws Exception {
        var mineskinVariant =
                switch (variant) {
                    case CLASSIC -> Variant.CLASSIC;
                    case SLIM -> Variant.SLIM;
                    case null -> Variant.AUTO;
                };

        var request = this.createGenerateRequest(uri)
                .variant(mineskinVariant)
                .name(MineskinService.SKIN_NAME)
                .visibility(Visibility.UNLISTED);

        var skin = this.mineskinClient
                .queue()
                .submit(request)
                .thenApply(QueueResponse::getJob)
                .thenCompose(jobInfo -> jobInfo.waitForCompletion(this.mineskinClient))
                .thenCompose(jobReference -> jobReference.getOrLoadSkin(this.mineskinClient))
                .join();

        return Optional.of(new Property(
                PlayerUtils.TEXTURES_KEY,
                skin.texture().data().value(),
                skin.texture().data().signature()));
    }

    private GenerateRequest createGenerateRequest(URI uri) throws Exception {
        if ("file".equals(uri.getScheme())) return GenerateRequest.upload(Files.newInputStream(Path.of(uri)));

        if (MineskinService.isHttpUrl(uri) && this.proxyUrlUpload)
            return GenerateRequest.upload(new ByteArrayInputStream(this.downloadImage(uri)));

        return GenerateRequest.url(uri);
    }

    private byte[] downloadImage(URI uri) throws IOException {
        var request = HttpRequest.newBuilder().uri(uri).GET().build();

        var response = WebUtils.executeRequest(request, HttpResponse.BodyHandlers.ofByteArray());
        WebUtils.throwOnClientErrors(response);

        if (response.statusCode() != 200) throw new IOException("unexpected status code " + response.statusCode());

        return response.body();
    }

    private static boolean isHttpUrl(URI uri) {
        return "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
    }
}
