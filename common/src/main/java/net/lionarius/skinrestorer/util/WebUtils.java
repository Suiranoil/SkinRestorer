package net.lionarius.skinrestorer.util;

import net.lionarius.skinrestorer.SkinRestorer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public final class WebUtils {
    
    private WebUtils() {}
    
    public static final String USER_AGENT;
    
    private static HttpClient HTTP_CLIENT = null;
    
    static {
        USER_AGENT = String.format("SkinRestorer/%d", System.currentTimeMillis() % 65535);
    }
    
    public static void recreateHttpClient() {
        HTTP_CLIENT = WebUtils.buildClient();
    }
    
    private static HttpClient buildClient() {
        var builder = HttpClient.newBuilder();
        
        var proxy = SkinRestorer.getConfig().proxy();
        proxy.ifPresent(value -> builder.proxy(ProxySelector.of(InetSocketAddress.createUnresolved(value.host(), value.port()))));
        
        try {
            builder.connectTimeout(Duration.of(SkinRestorer.getConfig().requestTimeout(), ChronoUnit.SECONDS));
        } catch (IllegalArgumentException e) {
            SkinRestorer.LOGGER.error("failed to set request timeout", e);
            builder.connectTimeout(Duration.of(10, ChronoUnit.SECONDS));
        }
        
        return builder.build();
    }
    
    public static HttpResponse<String> executeRequest(HttpRequest request) throws IOException {
        try {
            var modifiedRequest = HttpRequest.newBuilder(request, (name, value) -> true)
                    .header("User-Agent", WebUtils.USER_AGENT)
                    .build();
            
            final var response = WebUtils.HTTP_CLIENT.send(modifiedRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 500)
                throw new IOException("server error " + response.statusCode());
            
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
    
    public static void throwOnClientErrors(HttpResponse<?> response) {
        String message = switch (response.statusCode()) {
            case 400 -> "bad request";
            case 401 -> "unauthorized";
            case 403 -> "forbidden";
            case 404 -> "not found";
            case 405 -> "method not allowed";
            case 408 -> "request timeout";
            case 429 -> "too many requests";
            default -> null;
        };
        
        if (message != null)
            throw new IllegalStateException(message);
    }
}
