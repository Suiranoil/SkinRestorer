package net.lionarius.skinrestorer.util;

import net.lionarius.skinrestorer.SkinRestorer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public final class WebUtils {
    public static final String DEFAULT_USER_AGENT =
            String.format("SkinRestorer/%d", System.currentTimeMillis() % 65535);

    private static String USER_AGENT = WebUtils.DEFAULT_USER_AGENT;
    private static volatile HttpClient HTTP_CLIENT = null;

    private WebUtils() {}

    public static String getUserAgent() {
        return WebUtils.USER_AGENT;
    }

    public static void recreateHttpClient() {
        var configUserAgent = SkinRestorer.getConfig().request().userAgent();
        WebUtils.USER_AGENT = configUserAgent.isEmpty() ? WebUtils.DEFAULT_USER_AGENT : configUserAgent;

        var oldClient = WebUtils.HTTP_CLIENT;
        WebUtils.HTTP_CLIENT = WebUtils.buildClient();
        WebUtils.closeClient(oldClient);
    }

    public static void closeClient(HttpClient client) {
        // HttpClient is AutoCloseable on Java 21+ (it owns a selector + thread pool); closing the
        // replaced instance avoids leaking one per /skin config reload. On Java 17 this instanceof is
        // simply false and the method is a no-op. close() blocks until in-flight requests finish, so
        // run it off the reload thread.
        if (!(client instanceof AutoCloseable closeable)) return;

        var thread = new Thread(
                () -> {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        SkinRestorer.LOGGER.debug("Failed to close previous HTTP client", e);
                    }
                },
                "SkinRestorer-HttpClient-Close");
        thread.setDaemon(true);
        thread.start();
    }

    private static Duration getTimeoutDuration() {
        try {
            var timeout = Duration.of(SkinRestorer.getConfig().request().timeout(), ChronoUnit.SECONDS);
            if (timeout.isZero() || timeout.isNegative())
                throw new IllegalArgumentException("timeout must be positive");

            return timeout;
        } catch (IllegalArgumentException e) {
            SkinRestorer.LOGGER.error("Failed to set request timeout", e);
            return Duration.of(10, ChronoUnit.SECONDS);
        }
    }

    private static HttpClient buildClient() {
        var builder = HttpClient.newBuilder();

        var proxy = SkinRestorer.getConfig().request().proxy();
        proxy.ifPresent(value ->
                builder.proxy(ProxySelector.of(InetSocketAddress.createUnresolved(value.host(), value.port()))));

        builder.connectTimeout(WebUtils.getTimeoutDuration());

        return builder.build();
    }

    public static HttpResponse<String> executeRequest(HttpRequest request) throws IOException {
        return WebUtils.executeRequest(request, HttpResponse.BodyHandlers.ofString());
    }

    public static <T> HttpResponse<T> executeRequest(HttpRequest request, BodyHandler<T> bodyHandler)
            throws IOException {
        try {
            var modifiedRequest = HttpRequest.newBuilder(request, (name, value) -> true)
                    .header("User-Agent", WebUtils.getUserAgent())
                    .timeout(WebUtils.getTimeoutDuration())
                    .build();

            final var response = WebUtils.HTTP_CLIENT.send(modifiedRequest, bodyHandler);

            if (response.statusCode() >= 500) throw new IOException("server error " + response.statusCode());

            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    public static URI parseUri(String uri) {
        if (uri == null || uri.isEmpty()) return null;

        try {
            return URI.create(uri);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String ensureTrailingSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    // username validation allows characters like '/', '?', '#' and '"', so anything
    // player-provided must be encoded before being embedded into a URI
    public static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static void throwOnClientErrors(HttpResponse<?> response) {
        String message =
                switch (response.statusCode()) {
                    case 400 -> "bad request";
                    case 401 -> "unauthorized";
                    case 403 -> "forbidden";
                    case 404 -> "not found";
                    case 405 -> "method not allowed";
                    case 408 -> "request timeout";
                    case 429 -> "too many requests";
                    default -> null;
                };

        if (message != null) throw new IllegalStateException(message);
    }
}
